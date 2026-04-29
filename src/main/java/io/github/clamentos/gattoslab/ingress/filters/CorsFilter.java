package io.github.clamentos.gattoslab.ingress.filters;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.pojos.CorsConfig;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpUtils;

///..
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.StatusCodes;

///..
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

///
public final class CorsFilter {

    ///
    private final Set<HttpMethod> allowedMethods;
    private final Set<String> allowedOrigins;
    private final String maxAge;

    ///
    public CorsFilter(final ApplicationProperties applicationProperties) {

        final CorsConfig corsConfig = applicationProperties.getCorsConfig();

        allowedMethods = Arrays.stream(HttpMethod.values()).collect(Collectors.toSet());
        allowedOrigins = corsConfig.getAllowedOrigins();
        maxAge = Integer.toString(corsConfig.getMaxAge().toSecondsPart());
    }

    ///
    public boolean isAllowed(final HttpServerExchange exchange) throws IllegalArgumentException {

        final HeaderMap responseHeaders = exchange.getResponseHeaders();

        HttpUtils.addAllowedMethods(responseHeaders, allowedMethods);
        HttpUtils.addAllowedOrigins(responseHeaders, allowedOrigins);
        HttpUtils.addAllowCredentials(responseHeaders);
        HttpUtils.addCorsMaxAge(responseHeaders, maxAge);

        if(exchange.getAttachment(HttpUtils.DECODED_HTTP_METHOD) == HttpMethod.OPTIONS) {

            exchange.setStatusCode(StatusCodes.NO_CONTENT);
            exchange.endExchange();

            return false;
        }

        return true;
    }

    ///
}
