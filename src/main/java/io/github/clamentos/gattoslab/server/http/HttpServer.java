package io.github.clamentos.gattoslab.server.http;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.MutableString;
import io.github.clamentos.gattoslab.datastructures.StreamWriter;
import io.github.clamentos.gattoslab.datastructures.addresses.IpAddress;
import io.github.clamentos.gattoslab.exceptions.MalformedRequestException;
import io.github.clamentos.gattoslab.exceptions.RequestTooBigException;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.server.Server;
import io.github.clamentos.gattoslab.server.components.Acceptor;
import io.github.clamentos.gattoslab.server.components.SocketReader;
import io.github.clamentos.gattoslab.server.components.connection.Connection;
import io.github.clamentos.gattoslab.server.components.connection.ConnectionsTracker;
import io.github.clamentos.gattoslab.server.http.exchange.Filter;
import io.github.clamentos.gattoslab.server.http.exchange.Handler;
import io.github.clamentos.gattoslab.server.http.exchange.components.HttpExchange;
import io.github.clamentos.gattoslab.server.http.exchange.components.HttpHeader;
import io.github.clamentos.gattoslab.server.http.exchange.components.HttpMethod;
import io.github.clamentos.gattoslab.server.http.exchange.components.HttpStatus;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

///..
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;

///
public final class HttpServer extends Server {

    ///
    private static final byte[] RESPONSE_FOR_MALFORMED = (

        new String(HttpStatus.BAD_REQUEST.getResponseValue()) +
        GenericUtils.headerToString(ApplicationProperties.CLOSE_CONNECTION_HEADER) +
        "\r\n"

    ).getBytes();

    private static final byte[] RESPONSE_FOR_BAD_PROTOCOL = (

        new String(HttpStatus.HTTP_VERSION_NOT_SUPPORTED.getResponseValue()) +
        GenericUtils.headerToString(ApplicationProperties.CLOSE_CONNECTION_HEADER) +
        "\r\n"

    ).getBytes();

    private static final byte[] RESPONSE_FOR_TOO_BIG = (

        new String(HttpStatus.CONTENT_TOO_LARGE.getResponseValue()) +
        GenericUtils.headerToString(ApplicationProperties.CLOSE_CONNECTION_HEADER) +
        "\r\n"

    ).getBytes();

    ///.
    private final Acceptor[] siteAcceptors;
    private final Acceptor[] dashboardAcceptors;

    ///..
    private final AtomicBoolean isClosed;

    ///..
    private final Filter filter;
    private final Handler handler;

    ///..
    private final Set<String> httpProxyCustomHeaders;
    private final boolean isKeepAliveByDefault;

    ///
    public HttpServer(

        final SquashingLogger squashingLogger,
        final ConnectionsTracker connectionsTracker,
        final ApplicationProperties applicationProperties,
        final SSLContext sslContext,
        final Filter filter,
        final Handler handler

    ) throws IOException {

        super(new Logger(), squashingLogger, connectionsTracker);

        this.isClosed = new AtomicBoolean();

        this.filter = filter;
        this.handler = handler;

        this.httpProxyCustomHeaders = applicationProperties.getHttpProxyCustomHeaders();
        this.isKeepAliveByDefault = applicationProperties.isKeepAliveByDefault();

        this.siteAcceptors = this.createAcceptors(sslContext, applicationProperties.getSiteServers(), false);
        this.dashboardAcceptors = this.createAcceptors(sslContext, applicationProperties.getDashboardServers(), true);
    }

    ///
    @Override
    public void close() {

        this.logger.info("Begin shutdown...");
        this.isClosed.set(true);

        try {

            this.stopAcceptors(this.siteAcceptors, ApplicationProperties.SERVER_CLOSE_TIMEOUT);
            this.stopAcceptors(this.dashboardAcceptors, ApplicationProperties.SERVER_CLOSE_TIMEOUT);
        }

        catch(final InterruptedException _) {

            Thread.currentThread().interrupt();
            this.logger.error("Interrupted while closing, force quitting");
        }

        this.logger.info("End shutdown");
    }

    ///.
    private void accept(final ServerSocket serverSocket, final boolean isDashboard) {

        while(!this.isClosed.get()) {

            try {

                final Socket client = serverSocket.accept();
                GenericUtils.spawnVirtualThread("gattos-lab-http-handler", () -> this.handleConnection(client, isDashboard));
            }

            catch(final IOException | RuntimeException exc) {

                this.connectionsTracker.logException(exc);
            }
        }
    }

    ///..
    private void handleConnection(final Socket client, final boolean isDashboard) {

        final Connection connection = this.connectionsTracker.createConnection(client);
        final IpAddress remoteAddress = connection.getClientRemoteAddress();

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

                if(this.filter.filter(exchange, isDashboard)) this.handler.handle(exchange);
                if(!exchange.isKeepAlive() || exchange.isDoForceClose() || !this.isKeepAliveByDefault) break;

                final HttpStatus status = exchange.getResponseStatus();

                if(!doEnlargeReadTimeout && status != null && status.isSuccessfull()) {

                    client.setSoTimeout(keepAliveAmount);
                    doEnlargeReadTimeout = true;
                }

                reader.resetAllowed();
            }
        }

        catch(final IOException | RuntimeException exc) {

            if(exc instanceof RequestTooBigException && !connection.isSwept()) this.respondError(client, RESPONSE_FOR_TOO_BIG);
            this.connectionsTracker.logException(exc);
        }

        this.connectionsTracker.closeOrLog(client);
        this.connectionsTracker.removeConnection(Thread.currentThread(), remoteAddress);
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

        if(firstLineSplits.size() != 3) {

            this.respondError(client, RESPONSE_FOR_MALFORMED);
            throw new MalformedRequestException("Bad first line");
        }

        if(!"HTTP/1.1".equals(firstLineSplits.get(2))) {

            this.respondError(client, RESPONSE_FOR_BAD_PROTOCOL);
            throw new MalformedRequestException("Bad first line");
        }

        final Map<HttpHeader, String> headers = new EnumMap<>(HttpHeader.class);
        CharSequence line;

        while((line = reader.readLine()) != null && !line.isEmpty()) {

            final String[] headerSplits = this.splitHeader(line, client);
            final String key = headerSplits[0].toLowerCase(Locale.US);
            final HttpHeader header = HttpHeader.decode(key);

            if(header != null || this.httpProxyCustomHeaders.contains(key)) {

                headers.put(header, headerSplits[1]);
            }
        }

        return new HttpExchange(rawRemoteIpAddress, HttpMethod.decode(firstLineSplits.get(0)), firstLineSplits.get(1), headers, reader, writer);
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
    private void respondError(final Socket client, final byte[] message) {

        try {

            final OutputStream outputStream = client.getOutputStream();

            outputStream.write(message);
            outputStream.flush();
        }

        catch(final IOException exc) {

            this.connectionsTracker.logException(exc);
        }
    }

    ///..
    private Acceptor[] createAcceptors(final SSLContext sslContext, final Map<String, Integer> servers, final boolean isDashboard)
    throws IOException {

        final Acceptor[] acceptors = new Acceptor[servers.size()];
        int i = 0;

        for(final Entry<String, Integer> server : servers.entrySet()) {

            final String address = server.getKey();
            final int port = server.getValue();
            final boolean isSsl = port == 443 || port == 8443;
            final ServerSocketFactory serverSocketFactory = isSsl ? sslContext.getServerSocketFactory() : ServerSocketFactory.getDefault();

            this.logger.info("SSL for " + address + ":" + port + " is " + isSsl);

            final ServerSocket serverSocket = serverSocketFactory.createServerSocket(

                port,
                ApplicationProperties.SERVER_SOCKET_ACCEPT_QUEUE_SIZE,
                InetAddress.ofLiteral(address)
            );

            final Thread acceptorThread = GenericUtils.spawnVirtualThread(

                "gattos-lab-" + (isDashboard ? "dashboard" : "site") + "-acceptor-task-" + i,
                () -> this.accept(serverSocket, isDashboard)
            );

            acceptors[i] = new Acceptor(serverSocket, acceptorThread);
            i++;
        }

        return acceptors;
    }

    ///
}
