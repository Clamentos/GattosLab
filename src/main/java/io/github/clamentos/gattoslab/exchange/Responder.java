package io.github.clamentos.gattoslab.exchange;

///
import com.sun.net.httpserver.HttpExchange;

///..
import io.github.clamentos.gattoslab.http.BodyStreamer;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.Logger;
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
    protected void respond(final HttpExchange exchange, final HttpStatus status, final Map<String, List<String>> headers) {

        this.respond(exchange, status, headers, null, (byte[])null);
    }

    ///..
    protected void respond(

        final HttpExchange exchange,
        final HttpStatus status,
        final Map<String, List<String>> headers,
        final MimeType mimeType,
        final byte[] body
    ) {

        try {

            GenericUtils.respondSimple(exchange, status, headers, mimeType, body);
            this.observabilityService.requestEnded(exchange);
        }

        catch(final IOException exc) {

            this.logger.error("Could not respond because", exc);

            this.observabilityService.requestPartiallyEnded(exchange);
            exchange.close();
        }
    }

    ///..
    protected void respond(

        final HttpExchange exchange,
        final HttpStatus status,
        final Map<String, List<String>> headers,
        final MimeType mimeType,
        final BodyStreamer body
    ) {

        try {

            GenericUtils.respondSimple(exchange, status, headers, mimeType, body);
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
