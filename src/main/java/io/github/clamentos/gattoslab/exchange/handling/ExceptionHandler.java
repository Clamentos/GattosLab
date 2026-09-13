package io.github.clamentos.gattoslab.exchange.handling;

///
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.http.server.HttpExchange;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.IOException;
import java.util.Map;

///
public final class ExceptionHandler {

    ///
    private final Logger logger;

    ///..
    private final ObservabilityService observabilityService;

    ///
    public ExceptionHandler(final ObservabilityService observabilityService) {

        this.logger = new Logger();
        this.observabilityService = observabilityService;
    }

    ///
    public void handleUnexpected(final HttpExchange exchange, final Throwable exception) {

        if(exchange.isHandled()) {

            this.logger.warning(this.composeMessage(exchange, "but request was handled successfully. Exchange was: "), exception);
            exchange.close();
        }

        else {

            if(!exchange.isHandled()) {

                this.logger.error(this.composeMessage(exchange, "error when trying to send response. Exchange was: "), exception);

                if(!exchange.isTracked()) this.observabilityService.requestPartiallyEnded(exchange);
                exchange.close();
            }

            else {

                final HttpStatus status = switch(exception) {

                    case final IOException _ -> HttpStatus.INTERNAL_SERVER_ERROR;
                    case final IllegalArgumentException _ -> HttpStatus.BAD_REQUEST;
                    case final InterruptedException _ -> HttpStatus.SERVICE_UNAVAILABLE;

                    default -> {

                        logger.error("No switch case for exception", exception);
                        yield HttpStatus.INTERNAL_SERVER_ERROR;
                    }
                };

                try {

                    exchange.respond(status, Map.of(), MimeType.TEXT, exception.toString().getBytes());
                    if(!exchange.isTracked()) this.observabilityService.requestEnded(exchange);
                }

                catch(final IOException | RuntimeException exc) {

                    this.logger.error("Could not respond because", exc);
                    if(!exchange.isTracked()) this.observabilityService.requestPartiallyEnded(exchange);
                }

                exchange.close();
            }
        }
    }

    ///.
    private String composeMessage(final HttpExchange exchange, final String postfix) {

        return

            "Unhandled exception for request " +
            exchange.getRequestId() +
            ", " +
            postfix +
            GenericUtils.exchangeToString(exchange)
        ;
    }

    ///
}
