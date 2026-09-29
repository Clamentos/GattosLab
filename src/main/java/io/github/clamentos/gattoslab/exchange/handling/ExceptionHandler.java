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
    public void handleUncaught(final HttpExchange exchange, final Throwable exception) {

        if(exchange.isHandled()) {

            this.logger.warning(this.composeMessage(exchange, "but request was handled successfully"), exception);
            exchange.close();

            return;
        }

        boolean isPartial = false;

        if(!exchange.isHandled()) {

            this.logger.error(this.composeMessage(exchange, "error when trying to send response"), exception);
            isPartial = true;
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

            if(status == HttpStatus.INTERNAL_SERVER_ERROR) {

                this.logger.warning(this.composeMessage(exchange, "responded with 500 Internal Server Error"), exception);
            }

            try {

                exchange.respond(status, Map.of(), MimeType.TEXT, exception.toString().getBytes());
            }

            catch(final IOException | RuntimeException exc) {

                if(GenericUtils.isExceptionNotable(exception)) this.logger.error("Could not respond", exc);
                isPartial = true;
            }
        }

        if(!exchange.isTracked()) {

            if(isPartial) this.observabilityService.requestPartiallyEnded(exchange);
            else this.observabilityService.requestEnded(exchange);
        }

        exchange.close();
    }

    ///.
    private String composeMessage(final HttpExchange exchange, final String postfix) {

        return

            "Unhandled exception for request " +
            exchange.getRequestId() + ", " +
            postfix + ". Exchange is: " +
            GenericUtils.exchangeToString(exchange)
        ;
    }

    ///
}
