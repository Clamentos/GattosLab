package io.github.clamentos.gattoslab.exchange.handling;

///
import com.sun.net.httpserver.HttpExchange;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
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

        final Boolean isHandled = (Boolean)exchange.getAttribute(ApplicationProperties.REQUEST_HANDLED_ATTRIBUTE);

        if(isHandled == Boolean.TRUE) {

            this.logger.warning(this.composeMessage("', but request was handled successfully. Exchange was: ", exchange), exception);
            exchange.close();
        }

        else {

            final Boolean isTracked = (Boolean)exchange.getAttribute(ApplicationProperties.REQUEST_TRACKED_ATTRIBUTE);
            final boolean doObservability = isTracked != Boolean.TRUE;

            if(isHandled == Boolean.FALSE) {

                this.logger.error(this.composeMessage("', error when trying to send response. Exchange was", exchange), exception);

                if(doObservability) this.observabilityService.requestPartiallyEnded(exchange);
                exchange.close();
            }

            else {

                final HttpStatus status = switch(exception) {

                    case final HttpConnectTimeoutException _ -> HttpStatus.GATEWAY_TIMEOUT;
                    case final HttpTimeoutException _ -> HttpStatus.GATEWAY_TIMEOUT;
                    case final ConnectException _ -> HttpStatus.GATEWAY_TIMEOUT;

                    case final UnknownHostException _ -> HttpStatus.BAD_GATEWAY;
                    case final SocketException _ -> HttpStatus.BAD_GATEWAY;
                    case final EOFException _ -> HttpStatus.BAD_GATEWAY;
                    case final IOException _ -> HttpStatus.BAD_GATEWAY;

                    case final InterruptedException _ -> HttpStatus.SERVICE_UNAVAILABLE;

                    default -> {

                        logger.error("Uncaught exception", exception);
                        yield HttpStatus.INTERNAL_SERVER_ERROR;
                    }
                };

                try {

                    GenericUtils.respondSimple(exchange, status, Map.of(), MimeType.TEXT, exception.toString().getBytes());
                    if(doObservability) this.observabilityService.requestEnded(exchange);
                }

                catch(final IOException | RuntimeException exc) {

                    this.logger.error("Could not respond because", exc);
                    if(doObservability) this.observabilityService.requestPartiallyEnded(exchange);
                }

                exchange.close();
            }
        }
    }

    ///.
    private String composeMessage(final String postfix, final HttpExchange exchange) {

        return

            "Unhandled exception for request '" +
            exchange.getAttribute(ApplicationProperties.REQUEST_REQUEST_ID_ATTRIBUTE) +
            postfix +
            GenericUtils.exchangeToString(exchange)
        ;
    }

    ///
}
