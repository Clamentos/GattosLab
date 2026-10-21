package io.github.clamentos.gattoslab.http.server;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.MutableString;
import io.github.clamentos.gattoslab.datastructures.StreamWriter;
import io.github.clamentos.gattoslab.datastructures.address.IpAddress;
import io.github.clamentos.gattoslab.datastructures.address.IpV4;
import io.github.clamentos.gattoslab.datastructures.address.IpV6;
import io.github.clamentos.gattoslab.exceptions.MalformedRequestException;
import io.github.clamentos.gattoslab.exceptions.RequestTooBigException;
import io.github.clamentos.gattoslab.exchange.filters.Filter;
import io.github.clamentos.gattoslab.exchange.handling.Handler;
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.server.components.HttpExchange;
import io.github.clamentos.gattoslab.http.server.components.SocketReader;
import io.github.clamentos.gattoslab.http.server.components.connection.ConnectionsTracker;
import io.github.clamentos.gattoslab.http.server.components.connection.HttpConnection;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

///..
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

///
public final class HttpServer implements Closeable {

    ///
    private static final byte[] RESPONSE_FOR_MALFORMED = (

        new String(HttpStatus.BAD_REQUEST.getResponseValue()) +
        GenericUtils.headerToString(ApplicationProperties.CLOSE_CONNECTION_HEADER) +
        "\r\n"

    ).getBytes();

    private static final byte[] RESPONSE_FOR_TOO_BIG = (

        new String(HttpStatus.CONTENT_TOO_LARGE.getResponseValue()) +
        GenericUtils.headerToString(ApplicationProperties.CLOSE_CONNECTION_HEADER) +
        "\r\n"

    ).getBytes();

    private static final String TOO_MANY_SOCKETS_MESSAGE = "Too many sockets";
    private static final byte[] TOO_MANY_SOCKETS_MESSAGE_BYTES = TOO_MANY_SOCKETS_MESSAGE.getBytes();

    ///.
    private final Logger logger;
    private final SquashingLogger squashingLogger;

    ///..
    private final ServerSocket serverSocket;
    private final Thread acceptor;
    private final Thread sweeper;

    ///..
    private final ConnectionsTracker connectionsTracker;
    private final AtomicLong requestIdCounter;
    private final AtomicBoolean isClosed;

    ///..
    private final Filter filter;
    private final Handler handler;

    ///..
    private final boolean isKeepAliveByDefault;

    ///
    public HttpServer(

        final ApplicationProperties applicationProperties,
        final SquashingLogger squashingLogger,
        final InetAddress bindAddress,
        final SSLContext sslContext,
        final ConnectionsTracker connectionsTracker,
        final Filter filter,
        final Handler handler

    ) throws IOException {

        this.logger = new Logger();
        this.squashingLogger = squashingLogger;

        final int port = applicationProperties.getServerPort();
        final boolean isSsl = port == 443 || port == 8443;

        this.logger.info("Server SSL enabled: " + isSsl);

        final ServerSocketFactory serverSocketFactory = isSsl ? sslContext.getServerSocketFactory() : ServerSocketFactory.getDefault();
        this.serverSocket = serverSocketFactory.createServerSocket(port, ApplicationProperties.SERVER_SOCKET_ACCEPT_QUEUE_SIZE, bindAddress);

        this.connectionsTracker = connectionsTracker;
        this.requestIdCounter = new AtomicLong();
        this.isClosed = new AtomicBoolean();

        this.filter = filter;
        this.handler = handler;

        this.isKeepAliveByDefault = applicationProperties.isKeepAliveByDefault();

        this.acceptor = GenericUtils.spawnVirtualThread("gattos-lab-http-acceptor-task", this::accept);
        this.sweeper = GenericUtils.spawnVirtualThread("gattos-lab-http-sweeper-task", this::sweep);
    }

    ///
    @Override
    public void close() {

        this.logger.info("Begin shutdown...");

        final Duration timeout = ApplicationProperties.SERVER_CLOSE_TIMEOUT.dividedBy(3);
        this.isClosed.set(true);

        try {

            this.logger.info("Joining '" + this.acceptor.getName() + "'");
            this.acceptor.interrupt();
            if(!this.acceptor.join(timeout)) this.logger.warning("Timed-out while joining the acceptor thread");

            this.logger.info("Joining '" + this.sweeper.getName() + "'");
            this.sweeper.interrupt();
            if(!this.sweeper.join(timeout)) this.logger.warning("Timed-out while joining the sweeper thread");

            this.logger.info("Waiting for connections to finish...");

            final long waitForMillis = 100;
            long timeBudget = timeout.toMillis();

            while(this.connectionsTracker.getConnectionCount() > 0 && timeBudget > 0) {

                Thread.sleep(waitForMillis);
                timeBudget -= waitForMillis;
            }

            this.logger.info("Closing connections...");

            for(final HttpConnection connection : this.connectionsTracker.getConnections()) {

                connection.sweep();
                this.closeOrLog(connection.getSocket());
            }
        }

        catch(final InterruptedException _) {

            Thread.currentThread().interrupt();
            this.logger.error("Interrupted while closing, force quitting");
        }

        this.logger.info("End shutdown");
    }

    ///.
    private void accept() {

        while(!this.isClosed.get()) {

            try {

                final Socket client = this.serverSocket.accept();
                GenericUtils.spawnVirtualThread("gattos-lab-http-handler", () -> this.handleConnection(client));
            }

            catch(final IOException | RuntimeException exc) {

                this.logException(exc);
            }
        }
    }

    ///..
    private void sweep() {

        final long keepAliveDuration = ApplicationProperties.SERVER_MAX_KEEP_ALIVE_DURATION.toMillis();
        final long sleepAmount = ApplicationProperties.SOCKET_SWEEPER_POLL_PERIOD.toMillis();

        while(!this.isClosed.get()) {

            final long now = System.currentTimeMillis();

            for(final HttpConnection connection : this.connectionsTracker.getConnections()) {

                if(connection.getCreatedAt() + keepAliveDuration < now) {

                    connection.sweep();
                    this.closeOrLog(connection.getSocket());
                }
            }

            GenericUtils.silentSleep(sleepAmount);
        }
    }

    ///..
    private void handleConnection(final Socket client) {

        final IpAddress remoteAddress = this.isConnectionAllowed(client);
        if(remoteAddress == null) return;

        final HttpConnection connection = new HttpConnection(client);
        this.connectionsTracker.putConnection(Thread.currentThread(), connection);

        final int lingerAmount = (int)ApplicationProperties.SOCKET_LINGER.toSeconds();
        final int keepAliveAmount = (int)ApplicationProperties.SERVER_MAX_KEEP_ALIVE_DURATION.toMillis();

        boolean doEnlargeReadTimeout = false;

        try {

            client.setSoLinger(lingerAmount > 0, lingerAmount);
            client.setTcpNoDelay(ApplicationProperties.SOCKET_TCP_NO_DELAY);
            client.setSoTimeout((int)ApplicationProperties.SOCKET_PRELIMINARY_READ_TIMEOUT.toMillis());

            final SocketReader reader = new SocketReader(client.getInputStream(), ApplicationProperties.SERVER_INPUT_BUFFERS_SIZE);
            final StreamWriter writer = new StreamWriter(client.getOutputStream(), ApplicationProperties.SERVER_OUTPUT_BUFFERS_SIZE);

            CharSequence firstLine;

            while(connection.isValid() && (firstLine = reader.readLine()) != null) {

                final HttpExchange exchange = this.parseRequest(client, remoteAddress, reader, writer, firstLine);

                if(this.filter.filter(exchange)) this.handler.handle(exchange);
                if(!exchange.isKeepAlive() || exchange.isDoForceClose() || !this.isKeepAliveByDefault) break;

                if(!doEnlargeReadTimeout) {

                    client.setSoTimeout(keepAliveAmount);
                    doEnlargeReadTimeout = true;
                }

                reader.resetAllowed();
            }
        }

        catch(final IOException | RuntimeException exc) {

            if(exc instanceof RequestTooBigException && !connection.isSwept()) this.respondError(client, RESPONSE_FOR_TOO_BIG);
            this.logException(exc);
        }

        this.closeOrLog(client);
        this.connectionsTracker.removeConnection(Thread.currentThread(), remoteAddress);
    }

    ///..
    private IpAddress isConnectionAllowed(final Socket client) {

        final int currentConnectionCount = this.connectionsTracker.updateConnectionCount();
        final byte[] rawRemoteAddress = ((InetSocketAddress)client.getRemoteSocketAddress()).getAddress().getAddress();
        final IpAddress remoteAddress = rawRemoteAddress.length == 4 ? new IpV4(rawRemoteAddress) : new IpV6(rawRemoteAddress);
        final int currentIpConnectionCount = this.connectionsTracker.updateSocketsPerIpCount(remoteAddress);

        this.connectionsTracker.trackConnectionOpened();

        if(
            currentConnectionCount >= ApplicationProperties.MAX_SERVER_SOCKETS ||
            currentIpConnectionCount >= ApplicationProperties.MAX_SOCKETS_PER_IP
        ) {

            this.squashingLogger.warning(GenericUtils.composeMessageForSquash(TOO_MANY_SOCKETS_MESSAGE));

            this.respondError(client, TOO_MANY_SOCKETS_MESSAGE_BYTES);
            this.closeOrLog(client);
            this.connectionsTracker.removeSocketsPerIp(remoteAddress);

            return null;
        }

        return remoteAddress;
    }

    ///..
    private HttpExchange parseRequest(

        final Socket client,
        final IpAddress rawRemoteIpAddress,
        final SocketReader reader,
        final StreamWriter writer,
        final CharSequence firstLine

    ) throws IOException {

        final List<String> firstLineSplits = GenericUtils.fastSplit(firstLine, ' ');

        if(firstLineSplits.size() != 3 || !"HTTP/1.1".equals(firstLineSplits.get(2))) {

            this.respondError(client, RESPONSE_FOR_MALFORMED);
            throw new MalformedRequestException("Bad first line");
        }

        final Map<HttpHeader, String> headers = new EnumMap<>(HttpHeader.class);
        CharSequence line;

        while((line = reader.readLine()) != null && !line.isEmpty()) {

            final String[] headerSplits = this.splitHeader(line, client);
            final HttpHeader header = HttpHeader.decode(headerSplits[0]);

            if(header != null) headers.put(header, headerSplits[1]);
        }

        return new HttpExchange(

            this.requestIdCounter.getAndIncrement(),
            rawRemoteIpAddress,
            HttpMethod.decode(firstLineSplits.get(0)),
            firstLineSplits.get(1),
            headers,
            reader,
            writer
        );
    }

    ///..
    private String[] splitHeader(final CharSequence rawHeader, final Socket client) throws IOException {

        final String[] header = new String[]{null, ""};
        final int length = rawHeader.length();
        final MutableString mutableString = new MutableString(length);

        boolean separatorFound = false;
        int i = 0;

        while(i < length && !separatorFound) {

            final char currentChar = rawHeader.charAt(i);

            if(currentChar != ':') {

                mutableString.append(currentChar);
            }

            else {

                header[0] = mutableString.toString();
                separatorFound = true;
            }

            i++;
        }

        if(!separatorFound) {

            this.respondError(client, RESPONSE_FOR_MALFORMED);
            throw new MalformedRequestException("Bad header line");
        }

        if(i == length) return header;

        mutableString.clear();
        if(rawHeader.charAt(i) == ' ') i++;

        while(i < length) {

            mutableString.append(rawHeader.charAt(i++));
        }

        header[1] = mutableString.toString();
        return header;
    }

    ///..
    private void logException(final Exception exc) {

        if(!(exc instanceof SocketTimeoutException) && !(exc instanceof SSLException)) {

            if(exc instanceof MalformedRequestException || exc instanceof RequestTooBigException) {

                this.squashingLogger.warning(GenericUtils.composeMessageForSquash(exc.toString()));
            }

            else if(GenericUtils.isExceptionNotable(exc)) {

                this.logger.error("Uncaught exception", exc);
            }
        }
    }

    ///..
    private void closeOrLog(final Socket client) {

        try {

            client.close();
            this.connectionsTracker.trackConnectionClosed();
        }

        catch(final IOException exc) {

            this.logException(exc);
        }
    }

    ///..
    private void respondError(final Socket client, final byte[] message) {

        try {

            final OutputStream outputStream = client.getOutputStream();

            outputStream.write(message);
            outputStream.flush();
        }

        catch(final IOException exc) {

            this.logException(exc);
        }
    }

    ///
}
