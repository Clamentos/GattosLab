package io.github.clamentos.gattoslab.http.server;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

///..
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

///
public final class HttpServer implements Closeable {

    ///
    private final Logger logger;
    private final SquashingLogger squashingLogger;

    ///..
    private final ServerSocket serverSocket;
    private final Thread acceptor;
    private final Thread terminator;

    private final Map<Thread, HttpConnection> connections;
    private final AtomicLong requestIdCounter;
    private final AtomicBoolean isClosed;

    private final Filter[] filters;
    private final Handler handler;

    ///
    public HttpServer(

        final SquashingLogger squashingLogger,
        final InetAddress address,
        final int port,
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
        this.requestIdCounter = new AtomicLong();
        this.isClosed = new AtomicBoolean();

        this.filters = filters.stream().toArray(Filter[]::new);
        this.handler = handler;

        this.acceptor = GenericUtils.spawnVirtualThread("gattos-lab-http-acceptor-task", this::accept);
        this.terminator = GenericUtils.spawnVirtualThread("gattos-lab-http-sweeper-task", this::sweep);
    }

    ///
    @Override
    public void close() {

        final Duration timeout = ApplicationProperties.SERVER_CLOSE_TIMEOUT.dividedBy(3);
        this.isClosed.set(true);

        try {

            this.acceptor.interrupt();
            this.join(this.acceptor, timeout);

            this.terminator.interrupt();
            this.join(this.terminator, timeout);

            long timeBudget = timeout.toMillis();

            while(!this.connections.isEmpty() && timeBudget > 0) {

                Thread.sleep(100);
                timeBudget -= 100;
            }

            for(final HttpConnection connection : this.connections.values()) {

                connection.getSweeped().set(true);
                this.closeOrLog(connection.getSocket());
            }
        }

        catch(final InterruptedException _) {

            Thread.currentThread().interrupt();
            this.logger.error("Interrupted while closing");
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
        final long sleepAmount = ApplicationProperties.SERVER_SWEEPER_POLL_PERIOD.toMillis();

        while(!this.isClosed.get()) {

            final long now = System.currentTimeMillis();
            final Iterator<Entry<Thread, HttpConnection>> entries = this.connections.entrySet().iterator();

            while(entries.hasNext()) {

                final Entry<Thread, HttpConnection> entry = entries.next();
                final HttpConnection connection = entry.getValue();

                if(connection.getCreatedAt() + keepAliveDuration < now) {

                    connection.getSweeped().set(true);
                    this.closeOrLog(connection.getSocket());
                    entries.remove();
                }
            }

            GenericUtils.silentSleep(sleepAmount);
        }
    }

    ///..
    private void handleConnection(final Socket client) {

        if(this.connections.size() > 1024) this.closeOrLog(client);

        final Thread self = Thread.currentThread();
        final HttpConnection connection = new HttpConnection(client);

        this.connections.put(self, connection);

        try {

            final SocketReader reader = new SocketReader(client.getInputStream(), ApplicationProperties.SERVER_IO_BUFFERS_SIZE);
            final BufferedOutputStream writer = new BufferedOutputStream(client.getOutputStream(), ApplicationProperties.SERVER_IO_BUFFERS_SIZE);

            while(!client.isClosed() && !client.isInputShutdown() && !client.isOutputShutdown()) {

                reader.resetAllowed();

                final CharSequence firstLine = reader.readLine();
                if(firstLine == null) break;

                final HttpExchange exchange = this.parseRequest(client, reader, writer, firstLine);
                boolean doDispatch = true;

                for(final Filter filter : this.filters) {

                    if(!filter.filter(exchange)) {

                        doDispatch = false;
                        break;
                    }
                }

                if(doDispatch) this.handler.handle(exchange);

                final String connectionHeader = exchange.getRequestHeaders().get(HttpHeader.CONNECTION);
                if((connectionHeader != null && connectionHeader.contains("close")) || exchange.isForceClose()) break;
            }
        }

        catch(final IOException | RuntimeException exc) {

            if(!connection.getSweeped().get()) this.logException(exc);
        }

        this.closeOrLog(client);
        this.connections.remove(self);
    }

    ///..
    private HttpExchange parseRequest(final Socket socket, final SocketReader reader, final OutputStream writer, final CharSequence firstLine)
    throws IOException {

        final List<String> firstLineSplits = GenericUtils.fastSplit(firstLine, ' ');
        if(firstLineSplits.size() != 3) throw new IOException("Bad first line '" + firstLine + "'");

        final StringBuilder buffer = new StringBuilder(40);
        final Map<HttpHeader, String> headers = new EnumMap<>(HttpHeader.class);

        CharSequence line;

        while((line = reader.readLine()) != null) {

            if(line.isEmpty()) break;

            final List<String> headerSplits = GenericUtils.fastSplit(line, ':');
            if(headerSplits.size() < 2) throw new IOException("Bad header line '" + line + "'");

            final HttpHeader header = HttpHeader.decode(headerSplits.get(0), buffer);

            if(header != null) headers.put(header, headerSplits.get(1).substring(1));
            buffer.setLength(0);
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
    private void logException(final Exception exc) {

        if(!(exc instanceof IOException)) {

            this.logger.error("Uncaught exception", exc);
        }

        else if(exc instanceof SSLException) {

            this.squashingLogger.warning("SSL error " + ApplicationProperties.LOG_SQUASH_COUNTS_CHAR + " times");
        }

        else {

            final String message = exc.getMessage();

            if(message != null && !message.contains("closed") && !message.contains("reset") && !message.contains("interrupt")) {

                this.logger.error("Uncaught exception", exc);
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
    private void join(final Thread thread, final Duration duration) throws InterruptedException {

        if(!thread.join(duration)) this.logger.warning("Timed-out while joining");
    }

    ///
}
