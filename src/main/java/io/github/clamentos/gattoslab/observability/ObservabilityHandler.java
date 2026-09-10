package io.github.clamentos.gattoslab.observability;

///
import com.sun.net.httpserver.HttpExchange;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exchange.handling.BasicHandler;
import io.github.clamentos.gattoslab.exchange.handling.ExceptionHandler;
import io.github.clamentos.gattoslab.exchange.handling.components.Api;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;

///..
import java.io.IOException;
import java.util.Map;

///
public final class ObservabilityHandler extends BasicHandler {

    ///
    public ObservabilityHandler(final ObservabilityService observabilityService, final ExceptionHandler exceptionHandler) {

        super(observabilityService, exceptionHandler);
    }

    ///
    @Override
    protected void doHandle(final HttpExchange exchange) throws IOException {

        final Api api = (Api)exchange.getAttribute(ApplicationProperties.REQUEST_RESOURCE_ATTRIBUTE);

        if(api == null) {

            super.respondNotFound(exchange);
            return;
        }

        if(super.rejectMethodNotAllowed(exchange, HttpMethod.GET)) return;

        try {

            switch(api) {

                case GET_LOGS: super.respond(exchange, HttpStatus.OK, Map.of(), MimeType.TEXT, super.observabilityService.getLogs(exchange)); return;

                case GET_REQUEST_METRICS:

                    super.respond(exchange, HttpStatus.OK, Map.of(), MimeType.JSON, super.observabilityService.getRequestMetrics(exchange));

                return;

                case GET_SYSTEM_METRICS:

                    super.respond(exchange, HttpStatus.OK, Map.of(), MimeType.JSON, super.observabilityService.getSystemMetrics(exchange));

                return;

                case GET_CRAWL_METRICS:

                    super.respond(exchange, HttpStatus.OK, Map.of(), MimeType.TEXT, super.observabilityService.getCrawlMetrics(exchange));

                return;

                default: super.respondNotFound(exchange); return;
            }
        }

        catch(final IllegalArgumentException exc) {

            super.respond(exchange, HttpStatus.BAD_REQUEST, Map.of(), MimeType.TEXT, exc.getMessage().getBytes());
            exchange.close();
        }
    }

    ///
}
