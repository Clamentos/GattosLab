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
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

///..
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;

///
public final class HttpServer extends Server {

    ///
    private static final byte[] RESPONSE_FOR_MALFORMED = (

        new String(HttpStatus.BAD_REQUEST.getResponseValue()) +
        GenericUtils.headersToString(ApplicationProperties.CLOSE_CONNECTION_HEADER) +
        "\r\n"

    ).getBytes(StandardCharsets.ISO_8859_1);

    private static final byte[] RESPONSE_FOR_BAD_PROTOCOL = (

        new String(HttpStatus.HTTP_VERSION_NOT_SUPPORTED.getResponseValue()) +
        GenericUtils.headersToString(ApplicationProperties.CLOSE_CONNECTION_HEADER) +
        "\r\n"

    ).getBytes(StandardCharsets.ISO_8859_1);

    private static final byte[] RESPONSE_FOR_TOO_BIG = (

        new String(HttpStatus.CONTENT_TOO_LARGE.getResponseValue()) +
        GenericUtils.headersToString(ApplicationProperties.CLOSE_CONNECTION_HEADER) +
        "\r\n"

    ).getBytes(StandardCharsets.ISO_8859_1);

    ///.
    private final Acceptor[] siteAcceptors;
    private final Acceptor[] dashboardAcceptors;

    ///..
    private final Filter filter;
    private final Handler handler;

    ///..
    private final int preliminaryTimeout;
    private final int keepAliveTimeout;

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

        this.filter = filter;
        this.handler = handler;

        this.siteAcceptors = this.createAcceptors(sslContext, applicationProperties.getSiteServers(), false);
        this.dashboardAcceptors = this.createAcceptors(sslContext, applicationProperties.getDashboardServers(), true);

        this.preliminaryTimeout = (int)ApplicationProperties.HTTP_SERVER_PRELIMINARY_READ_TIMEOUT.toMillis();
        this.keepAliveTimeout = (int)ApplicationProperties.HTTP_SERVER_KEEP_ALIVE_DURATION.toMillis();
    }

    ///
    @Override
    protected void doClose() {

        try {

            super.stopAcceptors(this.siteAcceptors, ApplicationProperties.ACCEPTORS_SHUTDOWN_TIMEOUT);
            super.stopAcceptors(this.dashboardAcceptors, ApplicationProperties.ACCEPTORS_SHUTDOWN_TIMEOUT);
        }

        catch(final InterruptedException _) {

            Thread.currentThread().interrupt();
            super.logger.error("Interrupted while closing, force quitting");
        }
    }

    ///.
    private void accept(final ServerSocket serverSocket, final boolean isDashboard) {

        while(!super.isClosed.get()) {

            try {

                final Socket client = serverSocket.accept();
                GenericUtils.spawnVirtualThread("gattos-lab-http-handler", () -> this.handleConnection(client, isDashboard));
            }

            catch(final IOException | RuntimeException exc) {

                super.connectionsTracker.logException(exc);
            }
        }
    }

    ///..
    private void handleConnection(final Socket client, final boolean isDashboard) {

        final Connection connection = this.connectionsTracker.createConnection(client);
        if(connection == null) return;

        final IpAddress remoteAddress = connection.getClientRemoteAddress();
        final int lingerAmount = (int)ApplicationProperties.SOCKET_LINGER_DURATION.toSeconds();

        StreamWriter writer = null;
        boolean doEnlargeReadTimeout = false;

        try {

            client.setSoLinger(lingerAmount > 0, lingerAmount);
            client.setTcpNoDelay(ApplicationProperties.SOCKET_TCP_NO_DELAY);
            client.setSoTimeout(this.preliminaryTimeout);

            final SocketReader reader = new SocketReader(

                client.getInputStream(),
                ApplicationProperties.HTTP_SERVER_INPUT_BUFFERS_SIZE,
                ApplicationProperties.MAX_HTTP_SERVER_REQUEST_SIZE
            );

            writer = new StreamWriter(client.getOutputStream(), ApplicationProperties.HTTP_SERVER_OUTPUT_BUFFERS_SIZE);
            CharSequence firstLine;

            while(connection.isValid() && (firstLine = reader.readLine()) != null) {

                final HttpExchange exchange = this.parseRequest(remoteAddress, reader, writer, firstLine);

                if(connection.getAge() <= 1) exchange.flagForClose();
                if(this.filter.filter(exchange, isDashboard)) this.handler.handle(exchange);
                if(exchange.isDoForceClose()) break;

                final HttpStatus status = exchange.getResponseStatus();

                if(!doEnlargeReadTimeout && status != null && status.isSuccessfull()) {

                    client.setSoTimeout(this.keepAliveTimeout);
                    doEnlargeReadTimeout = true;
                }

                reader.resetAllowed();
            }
        }

        catch(final IOException | RuntimeException exc) {

            if(connection.getAge() > 0) {

                switch(exc) {

                    case final MalformedRequestException mre -> this.respondError(writer, mre.getResponse());
                    case final RequestTooBigException _ -> this.respondError(writer, RESPONSE_FOR_TOO_BIG);

                    default -> this.connectionsTracker.logException(exc);
                }
            }
        }

        super.connectionsTracker.removeConnection(connection);
    }

    ///..
    private HttpExchange parseRequest(

        final IpAddress rawRemoteIpAddress,
        final SocketReader reader,
        final StreamWriter writer,
        final CharSequence firstLine

    ) throws IOException {

        final List<String> firstLineSplits = GenericUtils.fastSplit(firstLine, ' ');

        if(firstLineSplits.size() != 3) throw new MalformedRequestException("Bad first line", RESPONSE_FOR_MALFORMED);
        if(!"HTTP/1.1".equals(firstLineSplits.get(2))) throw new MalformedRequestException("Protocol not HTTP/1.1", RESPONSE_FOR_BAD_PROTOCOL);

        final Map<HttpHeader, String> headers = new EnumMap<>(HttpHeader.class);
        CharSequence line;

        while((line = reader.readLine()) != null && !line.isEmpty()) {

            final String[] headerSplits = this.splitHeader(line);
            final HttpHeader header = HttpHeader.decode(headerSplits[0]);

            if(header != null) headers.put(header, headerSplits[1]);
        }

        return new HttpExchange(rawRemoteIpAddress, HttpMethod.decode(firstLineSplits.get(0)), firstLineSplits.get(1), headers, reader, writer);
    }

    ///..
    private String[] splitHeader(final CharSequence rawHeader) throws IOException {

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

                header[0] = mutableString.toString().toLowerCase(Locale.US);
                separatorFound = true;
            }

            i++;
        }

        if(!separatorFound) throw new MalformedRequestException("Bad header line", RESPONSE_FOR_MALFORMED);
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
    private void respondError(final StreamWriter writer, final byte[] message) {

        try {

            writer.write(message);
            writer.flush();
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

            super.logger.info("SSL for " + address + ":" + port + " is " + isSsl);

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
