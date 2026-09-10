package io.github.clamentos.gattoslab.exchange.filters;

///
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

///..
import io.github.clamentos.gattoslab.exchange.handling.ExceptionHandler;

///..
import java.io.IOException;

///
public class BasicFilter extends Filter {

    ///
    private final ExceptionHandler exceptionHandler;

    ///..
    private final IngressFilter ingressFilter;
    private final RateLimitFilter rateLimitFilter;
    private final AuthorizationFilter authorizationFilter;

    ///
    public BasicFilter(

        final ExceptionHandler exceptionHandler,
        final IngressFilter ingressFilter,
        final RateLimitFilter rateLimitFilter,
        final AuthorizationFilter authorizationFilter
    ) {

        this.exceptionHandler = exceptionHandler;

        this.ingressFilter = ingressFilter;
        this.rateLimitFilter = rateLimitFilter;
        this.authorizationFilter = authorizationFilter;
    }

    ///
    @Override
    public void doFilter(final HttpExchange exchange, final Chain chain) {

        try {

            if(!this.ingressFilter.isOk(exchange)) return;
            if(!this.rateLimitFilter.isOk(exchange)) return;
            if(!this.authorizationFilter.isOk(exchange)) return;

            chain.doFilter(exchange);
        }

        catch(final IOException | RuntimeException exc) {

            this.exceptionHandler.handleUnexpected(exchange, exc);
        }
    }

    ///..
    @Override
    public String description() {

        return "";
    }

    ///
}
