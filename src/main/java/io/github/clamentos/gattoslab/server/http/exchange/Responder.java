package io.github.clamentos.gattoslab.server.http.exchange;

///
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.server.http.exchange.components.HttpExchange;
import io.github.clamentos.gattoslab.server.http.exchange.components.HttpHeader;
import io.github.clamentos.gattoslab.server.http.exchange.components.HttpStatus;
import io.github.clamentos.gattoslab.server.http.exchange.components.MimeType;
import io.github.clamentos.gattoslab.server.http.exchange.components.body.ChunkedResponseBodyCallback;
import io.github.clamentos.gattoslab.server.http.exchange.components.body.ResponseBodyCallback;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.IOException;
import java.util.List;
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
    protected void respond(final HttpExchange exchange, final HttpStatus status, final Map<HttpHeader, String> headers) {

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
        final Map<HttpHeader, String> headers,
        final MimeType mimeType,
        final byte[] body
    ) {

        this.respond(exchange, status, headers, mimeType, (Object)body);
    }

    ///..
    protected void respond(final HttpExchange exchange, final HttpStatus status, final MimeType mimeType, final ChunkedResponseBodyCallback body) {

        this.respond(exchange, status, Map.of(), mimeType, body);
    }

    ///..
    protected void respond(

        final HttpExchange exchange,
        final HttpStatus status,
        final Map<HttpHeader, String> headers,
        final MimeType mimeType,
        final ChunkedResponseBodyCallback body
    ) {

        this.respond(exchange, status, headers, mimeType, (Object)body);
    }

    ///..
    protected void respondForProxy(

        final HttpExchange exchange,
        final CharSequence responseLine,
        final List<CharSequence> headers,
        final ResponseBodyCallback body
    ) {

        try {

            exchange.respondForProxy(responseLine, headers, body);
            this.observabilityService.requestEnded(exchange);
        }

        catch(final IOException exc) {

            if(GenericUtils.isExceptionNotable(exc)) this.logger.error("Could not respond", exc);

            this.observabilityService.requestPartiallyEnded(exchange);
            exchange.flagForClose();
        }
    }

    ///..
    private void respond(

        final HttpExchange exchange,
        final HttpStatus status,
        final Map<HttpHeader, String> headers,
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
            exchange.flagForClose();
        }
    }

    ///
}
