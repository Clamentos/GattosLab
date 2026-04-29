package io.github.clamentos.gattoslab.observability;

///
import io.github.clamentos.gattoslab.http.HttpUtils;
import io.github.clamentos.gattoslab.http.ResponseSender;
import io.github.clamentos.gattoslab.observability.filters.AggregatedSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.LogSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.RequestMetricsSearchFilter;
import io.github.clamentos.gattoslab.observability.logging.LogsService;
import io.github.clamentos.gattoslab.session.SessionService;

///..
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.StatusCodes;

///..
import lombok.RequiredArgsConstructor;

///..
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.json.JsonMapper;

///
@RequiredArgsConstructor

///
public final class ObservabilityController {

    ///
    private final ObservabilityService observabilityService;
    private final SessionService sessionService;
    private final LogsService logsService;
    private final JsonMapper jsonMapper;

    ///
    public void getRequestMetrics(final HttpServerExchange exchange, final JsonGenerator generator) throws Exception {

        final ResponseSender sender = observabilityService.getRequestMetrics(generator, jsonMapper.readValue(exchange.getInputStream(), RequestMetricsSearchFilter.class));
        this.finalizeCompressedResponse(exchange, sender);
    }

    ///..
    public void getInvocationMetrics(final HttpServerExchange exchange, final JsonGenerator generator) throws Exception {

        final ResponseSender sender = observabilityService.getInvocationMetrics(generator, jsonMapper.readValue(exchange.getInputStream(), RequestMetricsSearchFilter.class));
        this.finalizeCompressedResponse(exchange, sender);
    }

    ///..
    public void getSystemMetrics(final HttpServerExchange exchange, final JsonGenerator generator) throws Exception {

        final ResponseSender sender = observabilityService.getSystemMetrics(generator, jsonMapper.readValue(exchange.getInputStream(), AggregatedSearchFilter.class));
        this.finalizeCompressedResponse(exchange, sender);
    }

    ///..
    public void getSessionsMetadata(final HttpServerExchange exchange) throws JacksonException {

        HttpUtils.respondRest(exchange, StatusCodes.OK, jsonMapper.writeValueAsString(sessionService.getSessionsMetadata()), null);
    }

    ///..
    public void getLogs(final HttpServerExchange exchange, final JsonGenerator generator) throws Exception {

        final ResponseSender sender = logsService.getLogs(generator, jsonMapper.readValue(exchange.getInputStream(), LogSearchFilter.class));
        this.finalizeCompressedResponse(exchange, sender);
    }

    ///..
    public void getFallbackLogs(final HttpServerExchange exchange, final JsonGenerator generator) throws Exception {

        final ResponseSender sender = logsService.getFallbackLogs(generator);
        this.finalizeCompressedResponse(exchange, sender);
    }

    ///.
    private void finalizeCompressedResponse(final HttpServerExchange exchange, final ResponseSender sender) throws Exception {

        exchange.setStatusCode(StatusCodes.OK);

        final HeaderMap headers = exchange.getResponseHeaders();

        HttpUtils.addGzipEncoding(headers);
        HttpUtils.addNoCache(headers);

        sender.send();
    }

    ///
}
