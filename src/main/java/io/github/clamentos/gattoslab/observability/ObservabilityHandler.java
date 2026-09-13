package io.github.clamentos.gattoslab.observability;

///
import io.github.clamentos.gattoslab.exchange.handling.BasicHandler;
import io.github.clamentos.gattoslab.exchange.handling.ExceptionHandler;
import io.github.clamentos.gattoslab.exchange.handling.components.Api;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.http.server.HttpExchange;

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

        final Api api = (Api)exchange.getResource();
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

                default: super.respond(exchange, HttpStatus.NOT_FOUND); return;
            }
        }

        catch(final IllegalArgumentException exc) {

            super.respond(exchange, HttpStatus.BAD_REQUEST, Map.of(), MimeType.TEXT, exc.getMessage().getBytes());
            exchange.close();
        }
    }

    ///
}
