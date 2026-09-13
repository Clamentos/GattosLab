package io.github.clamentos.gattoslab.exchange;

///
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.http.server.HttpExchange;
import io.github.clamentos.gattoslab.http.server.ResponseBodyCallback;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.Logger;

///..
import java.io.IOException;
import java.util.Map;

///
public abstract class Responder {

    ///
    protected final Logger logger;

    ///..
    protected final ObservabilityService observabilityService;

    ///
    protected Responder(final ObservabilityService observabilityService) {

        this.logger = new Logger();
        this.observabilityService = observabilityService;
    }

    ///
    protected void respond(final HttpExchange exchange, final HttpStatus status) {

        this.respond(exchange, status, Map.of(), null, (byte[])null);
    }

    ///..
    protected void respond(final HttpExchange exchange, final HttpStatus status, final Map<HttpHeader, byte[]> headers) {

        this.respond(exchange, status, headers, null, (byte[])null);
    }

    ///..
    protected void respond(

        final HttpExchange exchange,
        final HttpStatus status,
        final Map<HttpHeader, byte[]> headers,
        final MimeType mimeType,
        final ResponseBodyCallback body
    ) {

        this.respond(exchange, status, headers, mimeType, (Object)body);
    }

    ///..
    protected void respond(

        final HttpExchange exchange,
        final HttpStatus status,
        final Map<HttpHeader, byte[]> headers,
        final MimeType mimeType,
        final Object body
    ) {

        try {

            exchange.respond(status, headers, mimeType, body);
            this.observabilityService.requestEnded(exchange);
        }

        catch(final IOException exc) {

            this.logger.error("Could not respond because", exc);
            this.observabilityService.requestPartiallyEnded(exchange);
            exchange.close();
        }
    }

    ///
}
