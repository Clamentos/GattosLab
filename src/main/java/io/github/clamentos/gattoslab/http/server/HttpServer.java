package io.github.clamentos.gattoslab.http.server;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.MutableString;
import io.github.clamentos.gattoslab.http.HttpHeaderName;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

        HttpStatus.BAD_REQUEST.getValueForResponse() +
        GenericUtils.headerToString(ApplicationProperties.CLOSE_CONNECTION_HEADER) +
        "\r\n"

    ).getBytes();

    private static final byte[] RESPONSE_FOR_TOO_BIG = (

        HttpStatus.CONTENT_TOO_LARGE.getValueForResponse() +
        GenericUtils.headerToString(ApplicationProperties.CLOSE_CONNECTION_HEADER) +
        "\r\n"

    ).getBytes();

    private static final String TOO_MANY_SOCKETS_MESSAGE = "Too many sockets";
    private static final byte[] TOO_MANY_SOCKETS_BYTES = TOO_MANY_SOCKETS_MESSAGE.getBytes();

    ///.
    private final Logger logger;
    private final SquashingLogger squashingLogger;

    ///..
    private final ServerSocket serverSocket;
    private final Thread acceptor;
    private final Thread sweeper;

    ///..
    private final Map<Thread, HttpConnection> connections;
    private final AtomicInteger connectionCounter;
    private final AtomicLong requestIdCounter;
    private final AtomicBoolean isClosed;

    ///..
    private final Filter[] filters;
    private final Handler handler;

    ///..
    private final boolean keepAliveByDefault;

    ///
    public HttpServer(

        final SquashingLogger squashingLogger,
        final InetAddress address,
        final int port,
        final boolean keepAliveByDefault,
        final SSLContext sslContext,
        final List<Filter> filters,
        final Handler handler

    ) throws IOException {

        this.logger = new Logger();
        this.squashingLogger = squashingLogger;

        final boolean isSsl = port == 443 || port == 8443;
        this.logger.info("Server SSL enabled: " + isSsl);

        final ServerSocketFactory serverSocketFactory = isSsl ? sslContext.getServerSocketFactory() : ServerSocketFactory.getDefault();
        this.serverSocket = serverSocketFactory.createServerSocket(port, ApplicationProperties.SERVER_SOCKET_ACCEPT_QUEUE_SIZE, address);

        this.connections = new ConcurrentHashMap<>();
        connectionCounter = new AtomicInteger();
        this.requestIdCounter = new AtomicLong();
        this.isClosed = new AtomicBoolean();

        this.filters = filters.stream().toArray(Filter[]::new);
        this.handler = handler;

        this.keepAliveByDefault = keepAliveByDefault;

        this.acceptor = GenericUtils.spawnVirtualThread("gattos-lab-http-acceptor-task", this::accept);
        this.sweeper = GenericUtils.spawnVirtualThread("gattos-lab-http-sweeper-task", this::sweep);
    }

    ///
    @Override
    public void close() {

        final Duration timeout = ApplicationProperties.SERVER_CLOSE_TIMEOUT.dividedBy(3);
        this.isClosed.set(true);

        try {

            this.acceptor.interrupt();
            if(!this.acceptor.join(timeout)) this.logger.warning("Timed-out while joining the acceptor thread");

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
            final Iterator<HttpConnection> iterator = this.connections.values().iterator();

            while(iterator.hasNext()) {

                final HttpConnection connection = iterator.next();

                if(connection.getCreatedAt() + keepAliveDuration < now) {

                    connection.getSwept().set(true);
                    this.closeOrLog(connection.getSocket());
                    iterator.remove();
                    this.connectionCounter.decrementAndGet();
                }
            }

            GenericUtils.silentSleep(sleepAmount);
        }
    }

    ///..
    private void handleConnection(final Socket client) {

        final int currentConnectionCount = this.connectionCounter.getAndUpdate((currentValue -> Math.min(

            currentValue + 1,
            ApplicationProperties.MAX_SERVER_SOCKETS
        )));

        if(currentConnectionCount >= ApplicationProperties.MAX_SERVER_SOCKETS) {

            this.squashingLogger.warning(GenericUtils.composeMessageForSquash(TOO_MANY_SOCKETS_MESSAGE));
            this.respondError(client, TOO_MANY_SOCKETS_BYTES);
            this.closeOrLog(client);

            return;
        }

        final Thread self = Thread.currentThread();
        final HttpConnection connection = new HttpConnection(client);

        this.connections.put(self, connection);

        final int lingerAmount = (int)ApplicationProperties.SOCKET_LINGER.toSeconds();
        final int keepAliveAmount = (int)ApplicationProperties.SERVER_MAX_KEEP_ALIVE_DURATION.toMillis();

        boolean readTimeoutFlag = false;

        try {

            client.setSoLinger(lingerAmount > 0, lingerAmount);
            client.setTcpNoDelay(ApplicationProperties.SOCKET_TCP_NO_DELAY);
            client.setSoTimeout((int)ApplicationProperties.SOCKET_READ_TIMEOUT.toMillis());

            final SocketReader reader = new SocketReader(client.getInputStream(), ApplicationProperties.SERVER_INPUT_BUFFERS_SIZE);
            final StreamWriter writer = new StreamWriter(client.getOutputStream(), ApplicationProperties.SERVER_OUTPUT_BUFFERS_SIZE);

            CharSequence firstLine;

            while(!client.isClosed() && !client.isInputShutdown() && !client.isOutputShutdown() && (firstLine = reader.readLine()) != null) {

                reader.resetAllowed();

                final HttpExchange exchange = this.parseRequest(client, reader, writer, firstLine);
                boolean doDispatch = true;

                for(final Filter filter : this.filters) {

                    if(!filter.filter(exchange)) {

                        doDispatch = false;
                        break;
                    }
                }

                if(doDispatch) this.handler.handle(exchange);
                if(!exchange.isKeepAlive() || exchange.isForceClose() || !this.keepAliveByDefault) break;

                if(!readTimeoutFlag) {

                    client.setSoTimeout(keepAliveAmount);
                    readTimeoutFlag = true;
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
    }

    ///..
    private HttpExchange parseRequest(final Socket socket, final SocketReader reader, final StreamWriter writer, final CharSequence firstLine)
    throws IOException {

        final List<String> firstLineSplits = GenericUtils.fastSplit(firstLine, ' ');

        if(firstLineSplits.size() != 3 || !"HTTP/1.1".equals(firstLineSplits.get(2))) {

            this.respondError(socket, RESPONSE_FOR_MALFORMED);
            throw new MalformedRequestException("Bad first line");
        }

        final Map<HttpHeaderName, String> headers = new EnumMap<>(HttpHeaderName.class);
        CharSequence line;

        while((line = reader.readLine()) != null && !line.isEmpty()) {

            final String[] headerSplits = this.splitHeader(line, socket);
            final HttpHeaderName header = HttpHeaderName.decode(headerSplits[0]);

            if(header != null) headers.put(header, headerSplits[1]);
        }

        return new HttpExchange(

            this.requestIdCounter.getAndIncrement(),
            ((InetSocketAddress)socket.getRemoteSocketAddress()).getAddress().getAddress(),
            HttpMethod.decode(firstLineSplits.get(0)),
            firstLineSplits.get(1),
            headers,
            reader,
            writer
        );
    }

    ///..
    private String[] splitHeader(final CharSequence rawHeader, final Socket socket) throws IOException {

        final String[] splits = new String[]{null, ""};
        final int length = rawHeader.length();
        final MutableString builder = new MutableString(length);

        boolean separatorFound = false;
        int i = 0;

        while(i < length && !separatorFound) {

            final char currentChar = rawHeader.charAt(i);

            if(currentChar != ':') {

                builder.append(currentChar);
            }

            else {

                splits[0] = builder.toString();
                separatorFound = true;
            }

            i++;
        }

        if(!separatorFound) {

            this.respondError(socket, RESPONSE_FOR_MALFORMED);
            throw new MalformedRequestException("Bad header line");
        }

        if(i == length) return splits;

        builder.clear();
        if(rawHeader.charAt(i) == ' ') i++;

        while(i < length) {

            builder.append(rawHeader.charAt(i++));
        }

        splits[1] = builder.toString();
        return splits;
    }

    ///..
    private void logException(final Exception exc) {

        if(!(exc instanceof SocketTimeoutException) && !(exc instanceof SSLException)) {

            switch(exc) {

                case final MalformedRequestException _ -> this.squashingLogger.warning(GenericUtils.composeMessageForSquash(exc.getMessage()));
                case final RequestTooBigException _ -> this.squashingLogger.warning(GenericUtils.composeMessageForSquash("Request too big"));
                case final IOException _  when GenericUtils.isExceptionNotable(exc) -> this.logger.error("Uncaught exception", exc);

                default -> this.logger.error("Uncaught exception", exc);
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
