package io.github.clamentos.gattoslab.observability;

///
import com.mongodb.MongoException;

///..
import io.github.clamentos.gattoslab.observability.filters.LogSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.RequestMetricsSearchFilter;
import io.github.clamentos.gattoslab.observability.logging.LogsService;
import io.github.clamentos.gattoslab.session.SessionService;
import io.github.clamentos.gattoslab.utils.HttpUtils;

///..
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.StatusCodes;

///..
import java.io.IOException;

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
    public void getRequestMetrics(final HttpServerExchange exchange, final JsonGenerator generator) throws JacksonException, MongoException {

        observabilityService.getRequestMetrics(generator, jsonMapper.readValue(exchange.getInputStream(), RequestMetricsSearchFilter.class));
        this.finalizeStreamingResponse(exchange);
    }

    ///..
    public void getInvocationMetrics(final HttpServerExchange exchange, final JsonGenerator generator) throws JacksonException, MongoException {

        observabilityService.getInvocationMetrics(generator, jsonMapper.readValue(exchange.getInputStream(), RequestMetricsSearchFilter.class));
        this.finalizeStreamingResponse(exchange);
    }

    ///..
    public void getSystemMetrics(final HttpServerExchange exchange, final JsonGenerator generator) throws JacksonException, MongoException {

        observabilityService.getSystemMetrics(generator, jsonMapper.readValue(exchange.getInputStream(), RequestMetricsSearchFilter.class));
        this.finalizeStreamingResponse(exchange);
    }

    ///..
    public void getSessionsMetadata(final HttpServerExchange exchange) throws JacksonException {

        HttpUtils.respondRest(exchange, StatusCodes.OK, jsonMapper.writeValueAsString(sessionService.getSessionsMetadata()), null);
    }

    ///..
    public void getLogs(final HttpServerExchange exchange, final JsonGenerator generator) throws JacksonException, MongoException {

        logsService.getLogs(generator, jsonMapper.readValue(exchange.getInputStream(), LogSearchFilter.class));
        this.finalizeStreamingResponse(exchange);
    }

    ///..
    public void getFallbackLogs(final HttpServerExchange exchange, final JsonGenerator generator) throws IOException, JacksonException, MongoException {

        logsService.getFallbackLogs(generator);
        this.finalizeStreamingResponse(exchange);
    }

    ///.
    private void finalizeStreamingResponse(final HttpServerExchange exchange) {

        exchange.setStatusCode(StatusCodes.OK);

        final HeaderMap headers = exchange.getResponseHeaders();

        HttpUtils.addGzipEncoding(headers);
        HttpUtils.addNoCache(headers);
    }

    ///
}
