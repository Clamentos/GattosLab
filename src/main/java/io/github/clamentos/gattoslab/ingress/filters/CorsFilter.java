package io.github.clamentos.gattoslab.ingress.filters;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exceptions.EarlyTerminationException;
import io.github.clamentos.gattoslab.exceptions.handling.GlobalExceptionHandler;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpUtils;

///..
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import io.undertow.util.HeaderMap;

///..
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

///..
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

///
public final class CorsFilter implements Filter {

    ///
    private final Set<HttpMethod> allowedMethods;
    private final Set<String> allowedOrigins;
    private final String maxAge;

    ///..
    private final GlobalExceptionHandler globalExceptionHandler;

    ///
    public CorsFilter(final ApplicationProperties applicationProperties, final GlobalExceptionHandler globalExceptionHandler) {

        allowedMethods = Arrays.stream(HttpMethod.values()).collect(Collectors.toSet());
        allowedOrigins = applicationProperties.getCorsAllowedOrigins();
        maxAge = Integer.toString(applicationProperties.getCorsMaxAge().toSecondsPart());

        this.globalExceptionHandler = globalExceptionHandler;
    }

    ///

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {

        final HttpServerExchange exchange = ((HttpServletRequestImpl)request).getExchange();

        try {

            this.doPropagate(exchange);
            chain.doFilter(request, response);
        }

        catch(EarlyTerminationException exc) {

            globalExceptionHandler.handle(exc, exchange);
        }
    }

    ///.
    public void doPropagate(final HttpServerExchange exchange) throws EarlyTerminationException {

        final HttpMethod method = exchange.getAttachment(HttpUtils.DECODED_HTTP_METHOD);
        final HeaderMap responseHeaders = exchange.getResponseHeaders();

        HttpUtils.addAllowedMethods(responseHeaders, allowedMethods);
        HttpUtils.addAllowedOrigins(responseHeaders, allowedOrigins);
        HttpUtils.addAllowCredentials(responseHeaders);
        HttpUtils.addCorsMaxAge(responseHeaders, maxAge);

        if(method == HttpMethod.OPTIONS) throw new EarlyTerminationException(HttpMethod.OPTIONS.name() + " request", "CorsFilter.doPropagate");
    }

    ///
}
