package io.github.clamentos.gattoslab.exchange;

///
import io.github.clamentos.gattoslab.http.HttpHeaderName;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.http.server.HttpExchange;
import io.github.clamentos.gattoslab.http.server.ResponseBodyCallback;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.utils.GenericUtils;

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
    protected void respond(final HttpExchange exchange, final HttpStatus status, final Map<HttpHeaderName, String> headers) {

        this.respond(exchange, status, headers, null, (byte[])null);
    }

    ///..
    protected void respond(final HttpExchange exchange, final HttpStatus status, final MimeType mimeType, final byte[] body) {

        this.respond(exchange, status, Map.of(), mimeType, body);
    }

    ///..
    protected void respond(

        final HttpExchange exchange,
        final HttpStatus status,
        final Map<HttpHeaderName, String> headers,
        final MimeType mimeType,
        final byte[] body
    ) {

        this.respond(exchange, status, headers, mimeType, (Object)body);
    }

    ///..
    protected void respond(final HttpExchange exchange, final HttpStatus status, final MimeType mimeType, final ResponseBodyCallback body) {

        this.respond(exchange, status, Map.of(), mimeType, body);
    }

    ///..
    protected void respond(

        final HttpExchange exchange,
        final HttpStatus status,
        final Map<HttpHeaderName, String> headers,
        final MimeType mimeType,
        final ResponseBodyCallback body
    ) {

        this.respond(exchange, status, headers, mimeType, (Object)body);
    }

    ///..
    private void respond(

        final HttpExchange exchange,
        final HttpStatus status,
        final Map<HttpHeaderName, String> headers,
        final MimeType mimeType,
        final Object body
    ) {

        try {

            exchange.respond(status, headers, mimeType, body);
            this.observabilityService.requestEnded(exchange);
        }

        catch(final IOException exc) {

            if(GenericUtils.isExceptionNotable(exc)) this.logger.error("Could not respond", exc);
            this.observabilityService.requestPartiallyEnded(exchange);
            exchange.close();
        }
    }

    ///
}
