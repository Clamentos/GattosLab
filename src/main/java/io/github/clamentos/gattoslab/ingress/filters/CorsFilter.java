package io.github.clamentos.gattoslab.ingress.filters;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.pojos.CorsConfig;
import io.github.clamentos.gattoslab.utils.HttpMethod;
import io.github.clamentos.gattoslab.utils.HttpUtils;

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
    private final int maxAge;

    ///
    public CorsFilter(final ApplicationProperties applicationProperties) {

        final CorsConfig corsConfig = applicationProperties.getCorsConfig();

        allowedMethods = Arrays.stream(HttpMethod.values()).collect(Collectors.toSet());
        allowedOrigins = corsConfig.getAllowedOrigins();
        maxAge = corsConfig.getMaxAge();
    }

    ///
    public void isAllowed(final HttpServerExchange exchange) {

        final HttpMethod method = HttpMethod.valueOf(exchange.getRequestMethod().toString());
        final HeaderMap responseHeaders = exchange.getResponseHeaders();

        HttpUtils.addAllowedMethods(responseHeaders, allowedMethods);
        HttpUtils.addAllowedOrigins(responseHeaders, allowedOrigins);
        HttpUtils.addAllowCredentials(responseHeaders);
        HttpUtils.addCorsMaxAge(responseHeaders, maxAge);

        if(method == HttpMethod.OPTIONS) {

            exchange.setStatusCode(StatusCodes.NO_CONTENT);
            exchange.endExchange();
        }
    }

    ///
}
