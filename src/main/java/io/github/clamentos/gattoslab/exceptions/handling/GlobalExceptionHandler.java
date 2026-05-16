package io.github.clamentos.gattoslab.exceptions.handling;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.exceptions.IllegalHttpMethodException;
import io.github.clamentos.gattoslab.exceptions.RedirectException;
import io.github.clamentos.gattoslab.exceptions.TooManyRequestsException;
import io.github.clamentos.gattoslab.exceptions.ValidationException;
import io.github.clamentos.gattoslab.http.HttpUtils;
import io.github.clamentos.gattoslab.observability.ObservabilityService;

///..
import io.undertow.io.UndertowOutputStream;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.StatusCodes;

///..
import java.io.IOException;

///..
import lombok.extern.slf4j.Slf4j;

///..
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

///
@Slf4j

///
public final class GlobalExceptionHandler {

    ///
    private final String retryAfterStr;

    ///..
    private final JsonMapper jsonMapper;
    private final ObservabilityService observabilityService;

    ///
    public GlobalExceptionHandler(final ApplicationProperties applicationProperties, final JsonMapper jsonMapper, final ObservabilityService observabilityService) {

        retryAfterStr = Long.toString(applicationProperties.getRateLimitRetryAfter().toSeconds());

        this.jsonMapper = jsonMapper;
        this.observabilityService = observabilityService;
    }

    ///
    public boolean handle(final Exception exception, final HttpServerExchange exchange) {

        return this.handleInternal(exception, exchange, false);
    }

    ///..
    public boolean handleWithReset(final Exception exception, final HttpServerExchange exchange) {

        return this.handleInternal(exception, exchange, true);
    }

    ///
    private boolean handleInternal(final Exception exception, final HttpServerExchange exchange, final boolean resetStream) {

        if(!exchange.isResponseStarted()) {

            try {

                if(resetStream) ((UndertowOutputStream)exchange.getOutputStream()).resetBuffer();

                switch(exception) {

                    case final ApiSecurityException ex -> this.respond(exchange, StatusCodes.FORBIDDEN, ex, "Forbidden");
                    case final IOException ex -> this.respond(exchange, StatusCodes.INTERNAL_SERVER_ERROR, ex, "File database error");
                    case final IllegalHttpMethodException ex -> this.respond(exchange, StatusCodes.METHOD_NOT_ALLOWED, ex, "Method not allowed");
                    case final JacksonException ex -> this.respond(exchange, StatusCodes.BAD_REQUEST, ex, "Nonsensical body");

                    case final RedirectException ex -> {

                        final HeaderMap redirect = HttpUtils.addRedirect(new HeaderMap(), ex.getMessage());
                        this.respond(exchange, StatusCodes.PERMANENT_REDIRECT, ex, "Redirect", redirect);
                    }

                    case final TooManyRequestsException ex -> {

                        final HeaderMap retryAfterHeader = HttpUtils.addRetryAfter(new HeaderMap(), retryAfterStr);
                        this.respond(exchange, StatusCodes.TOO_MANY_REQUESTS, ex, "Rate limit triggered", retryAfterHeader);
                    }

                    case final ValidationException ex -> this.respond(exchange, StatusCodes.BAD_REQUEST, ex, "Nonsensical body");
                    default -> this.respond(exchange, StatusCodes.INTERNAL_SERVER_ERROR, exception, "Unhandled error");
                }
            }

            catch(final RuntimeException exc) {

                log.error("Could not handle exception, will respond with a basic 500. REQID: {}", exchange.getRequestId(), exc);
                log.error("Original exception for REQID: {}", exchange.getRequestId(), exception);

                exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
            }
        }

        else {

            final String message = exception.getMessage();

            if(message != null && (message.contains("Broken pipe") || message.contains("Connection reset by peer"))) {

                exchange.putAttachment(HttpUtils.BROKEN_PIPE, true);
                observabilityService.updateRequestMetrics(exchange);

                return true;
            }

            else {

                log.warn("Response already started. Exception to be handled for REQID: {} is", exchange.getRequestId(), exception);
            }
        }

        return false;
    }

    ///.
    private void respond(final HttpServerExchange exchange, final int statusCode, final Throwable exception, final String title) throws JacksonException {

        HttpUtils.respondRest(exchange, statusCode, jsonMapper.writeValueAsString(new ErrorBody(title, exception, exchange)), null);
    }

    ///..
    private void respond(final HttpServerExchange exchange, final int statusCode, final Throwable exception, final String title, final HeaderMap extraHeaders)
    throws JacksonException {

        HttpUtils.respondRest(exchange, statusCode, jsonMapper.writeValueAsString(new ErrorBody(title, exception, exchange)), extraHeaders);
    }

    ///
}
