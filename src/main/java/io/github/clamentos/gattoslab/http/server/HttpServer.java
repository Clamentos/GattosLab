package io.github.clamentos.gattoslab.http.server;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.HashCodedByteArray;
import io.github.clamentos.gattoslab.datastructures.MutableString;
import io.github.clamentos.gattoslab.exceptions.MalformedRequestException;
import io.github.clamentos.gattoslab.exceptions.RequestTooBigException;
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpStatus;
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
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

///..
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

///
public final class HttpServer implements Closeable {

    ///
    private static final byte[] RESPONSE_FOR_MALFORMED = (

        new String(HttpStatus.BAD_REQUEST.getValueForResponse()) +
        GenericUtils.headerToString(ApplicationProperties.CLOSE_CONNECTION_HEADER) +
        "\r\n"

    ).getBytes();

    private static final byte[] RESPONSE_FOR_TOO_BIG = (

        new String(HttpStatus.CONTENT_TOO_LARGE.getValueForResponse()) +
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
    private final Map<Thread, HttpConnection> connections;
    private final Map<HashCodedByteArray, AtomicInteger> socketsPerIp;
    private final AtomicInteger connectionCounter;
    private final AtomicLong requestIdCounter;
    private final AtomicBoolean isClosed;

    ///..
    private final Filter[] filters;
    private final Handler handler;

    ///..
    private final boolean dokeepAliveByDefault;

    ///
    public HttpServer(

        final SquashingLogger squashingLogger,
        final InetAddress bindAddress,
        final int port,
        final boolean dokeepAliveByDefault,
        final SSLContext sslContext,
        final List<Filter> filters,
        final Handler handler

    ) throws IOException {

        this.logger = new Logger();
        this.squashingLogger = squashingLogger;

        final boolean isSsl = port == 443 || port == 8443;
        this.logger.info("Server SSL enabled: " + isSsl);

        final ServerSocketFactory serverSocketFactory = isSsl ? sslContext.getServerSocketFactory() : ServerSocketFactory.getDefault();
        this.serverSocket = serverSocketFactory.createServerSocket(port, ApplicationProperties.SERVER_SOCKET_ACCEPT_QUEUE_SIZE, bindAddress);

        this.connections = new ConcurrentHashMap<>();
        this.socketsPerIp = new ConcurrentHashMap<>();
        this.connectionCounter = new AtomicInteger();
        this.requestIdCounter = new AtomicLong();
        this.isClosed = new AtomicBoolean();

        this.filters = filters.stream().toArray(Filter[]::new);
        this.handler = handler;

        this.dokeepAliveByDefault = dokeepAliveByDefault;

        this.acceptor = GenericUtils.spawnVirtualThread("gattos-lab-http-acceptor-task", this::accept);
        this.sweeper = GenericUtils.spawnVirtualThread("gattos-lab-http-sweeper-task", this::sweep);
    }

    ///
    @Override
    public void close() {

        final Duration timeout = ApplicationProperties.SERVER_CLOSE_TIMEOUT.dividedBy(3);
        this.isClosed.set(true);

        try {

            this.logger.info("Joining '" + this.acceptor.getName() + "'");
            this.acceptor.interrupt();
            if(!this.acceptor.join(timeout)) this.logger.warning("Timed-out while joining the acceptor thread");

            this.logger.info("Joining '" + this.sweeper.getName() + "'");
            this.sweeper.interrupt();
            if(!this.sweeper.join(timeout)) this.logger.warning("Timed-out while joining the sweeper thread");

            long timeBudget = timeout.toMillis();

            while(!this.connections.isEmpty() && timeBudget > 0) {

                Thread.sleep(100);
                timeBudget -= 100;
            }

            for(final HttpConnection connection : this.connections.values()) {

                connection.getSwept().set(true);
                this.closeOrLog(connection.getSocket());
            }
        }

        catch(final InterruptedException _) {

            Thread.currentThread().interrupt();
            this.logger.error("Interrupted while closing, force quitting");
        }
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

            for(final Entry<Thread, HttpConnection> entry : this.connections.entrySet()) {

                final HttpConnection connection = entry.getValue();

                if(connection.getCreatedAt() + keepAliveDuration < now) {

                    connection.getSwept().set(true);
                    this.closeOrLog(connection.getSocket());
                }
            }

            GenericUtils.silentSleep(sleepAmount);
        }
    }

    ///..
    private void handleConnection(final Socket client) {

        final HashCodedByteArray rawRemoteIpAddress = this.isConnectionAllowed(client);
        if(rawRemoteIpAddress == null) return;

        final Thread self = Thread.currentThread();
        final HttpConnection connection = new HttpConnection(client);

        this.connections.put(self, connection);

        final int lingerAmount = (int)ApplicationProperties.SOCKET_LINGER.toSeconds();
        final int keepAliveAmount = (int)ApplicationProperties.SERVER_MAX_KEEP_ALIVE_DURATION.toMillis();

        boolean doEnlargeReadTimeout = false;

        try {

            client.setSoLinger(lingerAmount > 0, lingerAmount);
            client.setTcpNoDelay(ApplicationProperties.SOCKET_TCP_NO_DELAY);
            client.setSoTimeout((int)ApplicationProperties.SOCKET_READ_TIMEOUT.toMillis());

            final SocketReader reader = new SocketReader(client.getInputStream(), ApplicationProperties.SERVER_INPUT_BUFFERS_SIZE);
            final StreamWriter writer = new StreamWriter(client.getOutputStream(), ApplicationProperties.SERVER_OUTPUT_BUFFERS_SIZE);

            CharSequence firstLine;

            while(!client.isClosed() && !client.isInputShutdown() && !client.isOutputShutdown() && (firstLine = reader.readLine()) != null) {

                reader.resetAllowed();

                final HttpExchange exchange = this.parseRequest(client, rawRemoteIpAddress, reader, writer, firstLine);
                boolean doDispatch = true;

                for(final Filter filter : this.filters) {

                    if(!filter.filter(exchange)) {

                        doDispatch = false;
                        break;
                    }
                }

                if(doDispatch) this.handler.handle(exchange);
                if(!exchange.isKeepAlive() || exchange.isDoForceClose() || !this.dokeepAliveByDefault) break;

                if(!doEnlargeReadTimeout) {

                    client.setSoTimeout(keepAliveAmount);
                    doEnlargeReadTimeout = true;
                }
            }
        }

        catch(final IOException | RuntimeException exc) {

            if(exc instanceof RequestTooBigException && !connection.getSwept().get()) this.respondError(client, RESPONSE_FOR_TOO_BIG);
            this.logException(exc);
        }

        this.closeOrLog(client);
        this.connections.remove(self);
        this.connectionCounter.decrementAndGet();

        if(this.socketsPerIp.get(rawRemoteIpAddress).decrementAndGet() == 0) {

            this.socketsPerIp.remove(rawRemoteIpAddress);
        }
    }

    ///..
    private HashCodedByteArray isConnectionAllowed(final Socket client) {

        final int currentConnectionCount = this.connectionCounter.getAndUpdate((currentValue -> Math.min(

            currentValue + 1,
            ApplicationProperties.MAX_SERVER_SOCKETS
        )));

        final HashCodedByteArray remoteIp = new HashCodedByteArray(((InetSocketAddress)client.getRemoteSocketAddress()).getAddress().getAddress());
        final int currentIpConnectionCount = this.socketsPerIp.computeIfAbsent(remoteIp, _ -> new AtomicInteger()).incrementAndGet();

        if(
            currentConnectionCount >= ApplicationProperties.MAX_SERVER_SOCKETS ||
            currentIpConnectionCount >= ApplicationProperties.MAX_SOCKETS_PER_IP
        ) {

            this.squashingLogger.warning(GenericUtils.composeMessageForSquash(TOO_MANY_SOCKETS_MESSAGE));
            this.respondError(client, TOO_MANY_SOCKETS_MESSAGE_BYTES);
            this.closeOrLog(client);

            if(this.socketsPerIp.get(remoteIp).decrementAndGet() == 0) {

                this.socketsPerIp.remove(remoteIp);
            }

            return null;
        }

        return remoteIp;
    }

    ///..
    private HttpExchange parseRequest(
        
        final Socket socket,
        final HashCodedByteArray rawRemoteIpAddress,
        final SocketReader reader,
        final StreamWriter writer,
        final CharSequence firstLine

    ) throws IOException {

        final List<String> firstLineSplits = GenericUtils.fastSplit(firstLine, ' ');

        if(firstLineSplits.size() != 3 || !"HTTP/1.1".equals(firstLineSplits.get(2))) {

            this.respondError(socket, RESPONSE_FOR_MALFORMED);
            throw new MalformedRequestException("Bad first line");
        }

        final Map<HttpHeader, String> headers = new EnumMap<>(HttpHeader.class);
        CharSequence line;

        while((line = reader.readLine()) != null && !line.isEmpty()) {

            final String[] headerSplits = this.splitHeader(line, socket);
            final HttpHeader header = HttpHeader.decode(headerSplits[0]);

            if(header != null) headers.put(header, headerSplits[1]);
        }

        return new HttpExchange(

            this.requestIdCounter.getAndIncrement(),
            rawRemoteIpAddress.getData(),
            HttpMethod.decode(firstLineSplits.get(0)),
            firstLineSplits.get(1),
            headers,
            reader,
            writer
        );
    }

    ///..
    private String[] splitHeader(final CharSequence rawHeader, final Socket socket) throws IOException {

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

            this.respondError(socket, RESPONSE_FOR_MALFORMED);
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

            switch(exc) {

                case final MalformedRequestException _ -> this.squashingLogger.warning(GenericUtils.composeMessageForSquash(exc.getMessage()));
                case final RequestTooBigException _ -> this.squashingLogger.warning(GenericUtils.composeMessageForSquash("Request too big"));

                default -> {

                    if(GenericUtils.isExceptionNotable(exc)) this.logger.error("Uncaught exception", exc);
                }
            }
        }
    }

    ///..
    private void closeOrLog(final Socket socket) {

        try {

            socket.close();
        }

        catch(final IOException exc) {

            this.logException(exc);
        }
    }

    ///..
    private void respondError(final Socket socket, final byte[] message) {

        try {

            final OutputStream outputStream = socket.getOutputStream();

            outputStream.write(message);
            outputStream.flush();
        }

        catch(final IOException exc) {

            this.logException(exc);
        }
    }

    ///
}
