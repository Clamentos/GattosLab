package io.github.clamentos.gattoslab.exceptions.handling;

///
import com.mongodb.MongoException;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.exceptions.IllegalHttpMethodException;
import io.github.clamentos.gattoslab.exceptions.RedirectException;
import io.github.clamentos.gattoslab.exceptions.TooManyRequestsException;
import io.github.clamentos.gattoslab.utils.HttpUtils;

///..
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
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
    private final int retryAfter;

    ///..
    private final JsonMapper jsonMapper;

    ///
    public GlobalExceptionHandler(final ApplicationProperties applicationProperties, final JsonMapper jsonMapper) {

        retryAfter = applicationProperties.getRateLimitConfig().getRetryAfter() / 1000;
        this.jsonMapper = jsonMapper;
    }

    ///
    public void handle(final Exception exception, final HttpServerExchange exchange) {

        if(!exchange.isResponseStarted()) {

            try {

                switch(exception) {

                    case final ApiSecurityException ex -> this.respond(exchange, StatusCodes.FORBIDDEN, ex, "Forbidden");
                    case final IllegalHttpMethodException ex -> this.respond(exchange, StatusCodes.METHOD_NOT_ALLOWED, ex, "Method not allowed");

                    case final RedirectException ex -> {

                        HttpUtils.clearHeaders(exchange).add(HttpString.tryFromString("Location"), ex.getMessage());
                        exchange.setStatusCode(StatusCodes.PERMANENT_REDIRECT);
                    }

                    case final TooManyRequestsException ex -> HttpUtils.respondRest(

                        exchange,
                        StatusCodes.TOO_MANY_REQUESTS,
                        jsonMapper.writeValueAsString(new ErrorBody("Rate limit triggered", ex, exchange)),
                        HttpUtils.addRetryAfter(exchange.getResponseHeaders(), retryAfter)
                    );

                    case final MongoException ex -> this.respond(exchange, StatusCodes.INTERNAL_SERVER_ERROR, ex, "Database error");
                    default -> this.respond(exchange, StatusCodes.INTERNAL_SERVER_ERROR, exception, "Unhandled error");
                }
            }

            catch(final RuntimeException exc) {

                log.error("Could not handle exception, will respond with a basic 500", exc);
                exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
            }
        }
    }

    ///.
    private void respond(final HttpServerExchange exchange, final int statusCode, final Throwable exception, final String title) throws JacksonException {

        HttpUtils.respondRest(exchange, statusCode, jsonMapper.writeValueAsString(new ErrorBody(title, exception, exchange)), null);
    }

    ///
}
