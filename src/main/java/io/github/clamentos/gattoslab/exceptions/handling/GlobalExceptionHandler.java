package io.github.clamentos.gattoslab.exceptions.handling;

///
import com.mongodb.MongoException;

///..
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
import lombok.extern.slf4j.Slf4j;

///..
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

///
@Slf4j

///
public final class GlobalExceptionHandler {

    ///
    private final long retryAfter;

    ///..
    private final JsonMapper jsonMapper;
    private final ObservabilityService observabilityService;

    ///
    public GlobalExceptionHandler(final ApplicationProperties applicationProperties, final JsonMapper jsonMapper, final ObservabilityService observabilityService) {

        retryAfter = applicationProperties.getRateLimitConfig().getRetryAfter().toSeconds();

        this.jsonMapper = jsonMapper;
        this.observabilityService = observabilityService;
    }

    ///
    public boolean handle(final Exception exception, final HttpServerExchange exchange) {

        if(!exchange.isResponseStarted()) {

            try {

                switch(exception) {

                    case final ApiSecurityException ex -> this.respond(exchange, StatusCodes.FORBIDDEN, ex, "Forbidden");
                    case final IllegalHttpMethodException ex -> this.respond(exchange, StatusCodes.METHOD_NOT_ALLOWED, ex, "Method not allowed");
                    case final MongoException ex -> this.respond(exchange, StatusCodes.INTERNAL_SERVER_ERROR, ex, "Database error");

                    case final RedirectException ex -> {

                        final HeaderMap redirect = HttpUtils.addRedirect(new HeaderMap(), ex.getMessage());
                        this.respond(exchange, StatusCodes.PERMANENT_REDIRECT, ex, "Redirect", redirect);
                    }

                    case final TooManyRequestsException ex -> {

                        final HeaderMap retryAfterHeader = HttpUtils.addRetryAfter(new HeaderMap(), retryAfter);
                        this.respond(exchange, StatusCodes.TOO_MANY_REQUESTS, ex, "Rate limit triggered", retryAfterHeader);
                    }

                    case final ValidationException ex -> this.respond(exchange, StatusCodes.BAD_REQUEST, ex, "Nonsensical body");
                    default -> this.respond(exchange, StatusCodes.INTERNAL_SERVER_ERROR, exception, "Unhandled error");
                }
            }

            catch(final RuntimeException exc) {

                log.error("Could not handle exception, will respond with a basic 500", exc);
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

                log.warn("Response already started. Exception to be handled is", exception);
            }
        }

        return false;
    }

    ///.
    private void respond(final HttpServerExchange exchange, final int statusCode, final Throwable exception, final String title) throws JacksonException {

        ((UndertowOutputStream)exchange.getOutputStream()).resetBuffer();
        HttpUtils.respondRest(exchange, statusCode, jsonMapper.writeValueAsString(new ErrorBody(title, exception, exchange)), null);
    }

    ///..
    private void respond(final HttpServerExchange exchange, final int statusCode, final Throwable exception, final String title, final HeaderMap extraHeaders)
    throws JacksonException {

        ((UndertowOutputStream)exchange.getOutputStream()).resetBuffer();
        HttpUtils.respondRest(exchange, statusCode, jsonMapper.writeValueAsString(new ErrorBody(title, exception, exchange)), extraHeaders);
    }

    ///
}
