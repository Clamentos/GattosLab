package io.github.clamentos.gattoslab.exchange.handling;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.Pair;
import io.github.clamentos.gattoslab.exchange.Responder;
import io.github.clamentos.gattoslab.exchange.handling.components.Api;
import io.github.clamentos.gattoslab.exchange.handling.components.Resource;
import io.github.clamentos.gattoslab.exchange.handling.components.StaticResource;
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.http.server.HttpExchange;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.security.SecurityFailure;
import io.github.clamentos.gattoslab.security.SessionService;

///..
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

///
public final class Handler extends Responder {

    ///
    private static final byte[] NOT_FOUND_MESSAGE_BYTES = "Not found".getBytes();

    ///.
    private final SessionService sessionService;
    private final ExceptionHandler exceptionHandler;

    ///
    public Handler(

        final ObservabilityService observabilityService,
        final SessionService sessionService,
        final ExceptionHandler exceptionHandler
    ) {

        super(observabilityService);

        this.sessionService = sessionService;
        this.exceptionHandler = exceptionHandler;
    }

    ///
    public void handle(final HttpExchange exchange) {

        try {

            final Resource resource = exchange.getResource();

            if(resource == null) {

                super.respond(exchange, HttpStatus.NOT_FOUND, MimeType.TEXT, NOT_FOUND_MESSAGE_BYTES);
            }

            else if(resource instanceof StaticResource) {

                this.handleWebsite(exchange);
            }

            else {

                switch((Api)resource) {

                    case LOGIN, LOGOUT, GET_SESSIONS: this.handleSession(exchange); break;
                    case GET_LOGS, GET_REQUEST_METRICS, GET_SYSTEM_METRICS, GET_CRAWL_METRICS: this.handleObservability(exchange); break;
                }
            }
        }

        catch(final IOException | RuntimeException exc) {

            this.exceptionHandler.handleUncaught(exchange, exc);
        }
    }

    ///
    private void handleWebsite(final HttpExchange exchange) {

        final StaticResource resource = (StaticResource)exchange.getResource();
        if(this.rejectMethodNotAllowed(exchange, HttpMethod.GET)) return;

        final String diskPath = resource.getDiskPath();

        final Map<HttpHeader, String> headers = resource.isCacheable() ?

            ApplicationProperties.GZIP_CACHE_HEADERS :
            ApplicationProperties.GZIP_HEADER
        ;

        if(diskPath != null) {

            final InputStream rawDiskData = Handler.class.getClassLoader().getResourceAsStream(diskPath);

            if(rawDiskData == null) {

                super.respond(exchange, HttpStatus.NOT_FOUND);
                return;
            }

            super.respond(

                exchange,
                HttpStatus.OK,
                headers,
                resource.getMimeType(),

                writer -> {

                    try(final GZIPOutputStream compressor = new GZIPOutputStream(writer); rawDiskData) {

                        rawDiskData.transferTo(compressor);
                        compressor.finish();
                    }
                }
            );
        }

        else {

            super.respond(exchange, HttpStatus.OK, headers, resource.getMimeType(), resource.getCompressedContent());
        }
    }

    ///..
    private void handleSession(final HttpExchange exchange) {

        final Api api = (Api)exchange.getResource();

        switch(api) {

            case LOGIN:

                if(this.rejectMethodNotAllowed(exchange, HttpMethod.POST)) return;

                final Pair<SecurityFailure, Long> loginResult = this.sessionService.login(exchange);
                final SecurityFailure securityFailure = loginResult.getA();

                if(securityFailure == null) {

                    super.respond(exchange, HttpStatus.OK, MimeType.TEXT, Long.toString(loginResult.getB()).getBytes());
                }

                else {

                    super.respond(exchange, HttpStatus.UNAUTHORIZED, MimeType.TEXT, securityFailure.getMessage());
                    exchange.close();
                }

            return;

            case LOGOUT:

                if(this.rejectMethodNotAllowed(exchange, HttpMethod.DELETE)) return;

                this.sessionService.logout(exchange);
                super.respond(exchange, HttpStatus.OK, ApplicationProperties.CLEAR_SITE_DATA_HEADERS);
                exchange.close();

            return;

            case GET_SESSIONS:

                if(this.rejectMethodNotAllowed(exchange, HttpMethod.GET)) return;
                super.respond(exchange, HttpStatus.OK, MimeType.TEXT, this.sessionService.getSessions());

            return;

            default: super.respond(exchange, HttpStatus.NOT_FOUND); return;
        }
    }

    ///..
    private void handleObservability(final HttpExchange exchange) throws IOException {

        final Api api = (Api)exchange.getResource();
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

            super.respond(

                exchange,
                exc instanceof IOException ? HttpStatus.UNPROCESSABLE : HttpStatus.BAD_REQUEST,
                MimeType.TEXT,
                exc.getMessage().getBytes()
            );

            exchange.close();
        }
    }

    ///..
    private boolean rejectMethodNotAllowed(final HttpExchange exchange, final HttpMethod method) {

        if(exchange.getMethod() != method) {

            super.respond(exchange, HttpStatus.METHOD_NOT_ALLOWED, MimeType.TEXT, method.getAllowedBody());
            exchange.close();

            return true;
        }

        return false;
    }

    ///
}
