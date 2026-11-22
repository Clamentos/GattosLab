package io.github.clamentos.gattoslab.server.http.exchange;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.components.HttpProxy;
import io.github.clamentos.gattoslab.datastructures.Pair;
import io.github.clamentos.gattoslab.datastructures.StreamWriter;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.security.SessionService;
import io.github.clamentos.gattoslab.security.components.SecurityFailure;
import io.github.clamentos.gattoslab.server.components.SocketReader;
import io.github.clamentos.gattoslab.server.components.connection.ConnectionsTracker;
import io.github.clamentos.gattoslab.server.components.connection.ProxyConnection;
import io.github.clamentos.gattoslab.server.http.exchange.components.HttpExchange;
import io.github.clamentos.gattoslab.server.http.exchange.components.HttpHeader;
import io.github.clamentos.gattoslab.server.http.exchange.components.HttpMethod;
import io.github.clamentos.gattoslab.server.http.exchange.components.HttpStatus;
import io.github.clamentos.gattoslab.server.http.exchange.components.MimeType;
import io.github.clamentos.gattoslab.server.http.exchange.components.body.ChunkedResponseBodyCallback;
import io.github.clamentos.gattoslab.server.http.exchange.components.body.ResponseBodyCallback;
import io.github.clamentos.gattoslab.server.http.exchange.components.resources.Api;
import io.github.clamentos.gattoslab.server.http.exchange.components.resources.Resource;
import io.github.clamentos.gattoslab.server.http.exchange.components.resources.StaticResource;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPOutputStream;

///
public final class Handler extends Responder {

    ///
    private final SessionService sessionService;
    private final ExceptionHandler exceptionHandler;

    ///..
    private final ConnectionsTracker connectionsTracker;

    ///
    public Handler(

        final ObservabilityService observabilityService,
        final SessionService sessionService,
        final ExceptionHandler exceptionHandler,
        final ConnectionsTracker connectionsTracker
    ) {

        super(observabilityService);

        this.sessionService = sessionService;
        this.exceptionHandler = exceptionHandler;

        this.connectionsTracker = connectionsTracker;
    }

    ///
    public void handle(final HttpExchange exchange) {

        try {

            final HttpProxy proxy = exchange.getProxy();
            final Resource resource = exchange.getResource();

            if(proxy != null) {

                this.handleProxy(exchange, proxy);
            }

            else if(resource instanceof final StaticResource staticResource) {

                this.handleWebsite(exchange, staticResource);
            }

            else {

                final Api api = (Api)resource;

                switch(api) {

                    case LOGIN, LOGOUT, GET_SESSIONS: this.handleSession(exchange, api); break;
                    case GET_LOGS, GET_REQUEST_METRICS, GET_SYSTEM_METRICS, GET_CRAWL_METRICS: this.handleObservability(exchange, api); break;
                }
            }
        }

        catch(final IOException | RuntimeException exc) {

            this.exceptionHandler.handleUncaught(exchange, exc);
        }
    }

    ///
    private void handleProxy(final HttpExchange exchange, final HttpProxy proxy) {

        boolean requestSent = false;
        ProxyConnection connection = null;

        try {

            connection = this.connectionsTracker.getPooledHttpConnection(proxy);

            final SocketReader proxyReader = connection.getReader();
            final StreamWriter proxyWriter = connection.getWriter();
            final Map<HttpHeader, String> requestHeaders = exchange.getRequestHeaders();

            requestHeaders.put(HttpHeader.CONNECTION, "keep-alive");
            proxyWriter.write((exchange.getMethod().name() + " " + exchange.getUri() + " HTTP/1.1\r\n"));

            for(final Entry<HttpHeader, String> header : requestHeaders.entrySet()) {

                proxyWriter.write(header.getKey().getResponseValue());
                proxyWriter.write(header.getValue().getBytes(StandardCharsets.ISO_8859_1));
                proxyWriter.write(GenericUtils.NEW_LINE_BYTES);
            }

            proxyWriter.write(GenericUtils.NEW_LINE_BYTES);

            final String requestBodyLength = requestHeaders.get(HttpHeader.CONTENT_LENGTH);
            final int requestLength = requestBodyLength != null && !requestBodyLength.isEmpty() ? Integer.parseInt(requestBodyLength) : 0;

            exchange.getReader().transferTo(proxyWriter, requestLength);
            requestSent = true;

            final CharSequence responseLine = proxyReader.readLine();
            final List<CharSequence> responseHeaders = new ArrayList<>();
            CharSequence line;

            while((line = proxyReader.readLine()) != null && !line.isEmpty()) {

                responseHeaders.add(line);
            }

            super.respondForProxy(

                exchange,
                responseLine,
                responseHeaders,
                (ResponseBodyCallback)proxyReader::transferTo
            );
        }

        catch(final IOException exc) {

            if(requestSent) this.exceptionHandler.handleUncaught(exchange, exc);
            else super.respond(exchange, HttpStatus.SERVICE_UNAVAILABLE);
        }

        if(connection != null) connection.release();
    }

    ///..
    private void handleWebsite(final HttpExchange exchange, final StaticResource resource) {

        if(this.rejectMethodNotAllowed(exchange, HttpMethod.GET)) return;

        final Path diskPath = resource.getDiskPath();
        final Map<HttpHeader, String> headers = resource.isCacheable() ? ApplicationProperties.GZIP_CACHE_HEADERS : ApplicationProperties.GZIP_HEADER;

        if(diskPath != null) {

            try(final InputStream rawDiskData = Files.newInputStream(diskPath)) {

                if(rawDiskData == null) {

                    super.respond(exchange, HttpStatus.NOT_FOUND);
                    return;
                }

                super.respond(exchange, HttpStatus.OK, headers, resource.getMimeType(),

                    (ChunkedResponseBodyCallback)writer -> {

                        try(final GZIPOutputStream compressor = new GZIPOutputStream(writer); rawDiskData) {

                            rawDiskData.transferTo(compressor);
                            compressor.finish();
                        }
                    }
                );
            }

            catch(final IOException exc) {

                super.respond(exchange, HttpStatus.UNPROCESSABLE, MimeType.TEXT, exc.toString().getBytes(StandardCharsets.ISO_8859_1));
            }
        }

        else {

            super.respond(exchange, HttpStatus.OK, headers, resource.getMimeType(), resource.getCompressedContent());
        }
    }

    ///..
    private void handleSession(final HttpExchange exchange, final Api api) {

        switch(api) {

            case LOGIN:

                if(this.rejectMethodNotAllowed(exchange, HttpMethod.POST)) return;

                final Pair<SecurityFailure, Long> loginResult = this.sessionService.login(exchange);
                final SecurityFailure securityFailure = loginResult.getA();

                if(securityFailure == null) {

                    super.respond(exchange, HttpStatus.OK, MimeType.TEXT, Long.toString(loginResult.getB()).getBytes(StandardCharsets.ISO_8859_1));
                }

                else {

                    exchange.flagForClose();
                    super.respond(exchange, HttpStatus.UNAUTHORIZED, MimeType.TEXT, securityFailure.getMessage());
                }

            return;

            case LOGOUT:

                if(this.rejectMethodNotAllowed(exchange, HttpMethod.DELETE)) return;

                this.sessionService.logout(exchange);

                exchange.flagForClose();
                super.respond(exchange, HttpStatus.OK, ApplicationProperties.CLEAR_SITE_DATA_HEADERS);

            return;

            case GET_SESSIONS:

                if(this.rejectMethodNotAllowed(exchange, HttpMethod.GET)) return;
                super.respond(exchange, HttpStatus.OK, MimeType.TEXT, this.sessionService.getSessions());

            return;

            default: super.respond(exchange, HttpStatus.NOT_FOUND); return;
        }
    }

    ///..
    private void handleObservability(final HttpExchange exchange, final Api api) throws IOException {

        if(this.rejectMethodNotAllowed(exchange, HttpMethod.GET)) return;

        try {

            switch(api) {

                case GET_LOGS: super.respond(exchange, HttpStatus.OK, MimeType.TEXT, super.observabilityService.getLogs(exchange)); return;

                case GET_REQUEST_METRICS:

                    super.respond(exchange, HttpStatus.OK, MimeType.JSON, super.observabilityService.getRequestMetrics(exchange));

                return;

                case GET_SYSTEM_METRICS:

                    super.respond(exchange, HttpStatus.OK, MimeType.JSON, super.observabilityService.getSystemMetrics(exchange));

                return;

                case GET_CRAWL_METRICS:

                    super.respond(exchange, HttpStatus.OK, MimeType.TEXT, super.observabilityService.getCrawlMetrics(exchange));

                return;

                default: super.respond(exchange, HttpStatus.NOT_FOUND); return;
            }
        }

        catch(final IOException | IllegalArgumentException exc) {

            exchange.flagForClose();

            final HttpStatus status = exc instanceof IOException ? HttpStatus.UNPROCESSABLE : HttpStatus.BAD_REQUEST;
            super.respond(exchange, status, MimeType.TEXT, exc.toString().getBytes(StandardCharsets.ISO_8859_1));
        }
    }

    ///..
    private boolean rejectMethodNotAllowed(final HttpExchange exchange, final HttpMethod method) {

        if(exchange.getMethod() != method) {

            exchange.flagForClose();
            super.respond(exchange, HttpStatus.METHOD_NOT_ALLOWED, MimeType.TEXT, method.getMessage());

            return true;
        }

        return false;
    }

    ///
}
