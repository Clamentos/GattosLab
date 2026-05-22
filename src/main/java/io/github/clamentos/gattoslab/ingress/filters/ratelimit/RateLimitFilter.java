package io.github.clamentos.gattoslab.ingress.filters.ratelimit;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exceptions.TooManyRequestsException;
import io.github.clamentos.gattoslab.exceptions.handling.GlobalExceptionHandler;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;

///..
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.spec.HttpServletRequestImpl;

///..
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

///..
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

///..
import lombok.extern.slf4j.Slf4j;

///
@Slf4j

///
public final class RateLimitFilter implements Filter {

    ///
    private final int maxTokensPerIp;
    private final int blockCounterStart;

    ///..
    private final GlobalExceptionHandler globalExceptionHandler;

    ///..
    private final Map<InetAddress, RateLimitEntry> tokensByIp;

    ///
    public RateLimitFilter(

        final ApplicationProperties applicationProperties,
        final BatchScheduler batchScheduler,
        final GlobalExceptionHandler globalExceptionHandler

    )throws IllegalArgumentException {

        final int schedule = (int)batchScheduler.schedule(this::replenish, "RateLimitFilter::replenish", applicationProperties.getRateLimitReplenishRate());

        maxTokensPerIp = applicationProperties.getRateLimitMaxTokensPerIp();
        blockCounterStart = (int)(applicationProperties.getRateLimitRetryAfter().toMillis() / schedule);

        this.globalExceptionHandler = globalExceptionHandler;

        tokensByIp = new ConcurrentHashMap<>();
    }

    ///
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {

        final HttpServerExchange exchange = ((HttpServletRequestImpl)request).getExchange();

        try {

            this.isAllowed(exchange);
            chain.doFilter(request, response);
        }

        catch(final TooManyRequestsException exc) {

            globalExceptionHandler.handle(exc, exchange);
        }
    }

    ///.
    private void isAllowed(final HttpServerExchange exchange) throws TooManyRequestsException {

        final InetAddress ip = exchange.getSourceAddress().getAddress();
        final RateLimitEntry entry = tokensByIp.computeIfAbsent(ip, _ -> new RateLimitEntry(maxTokensPerIp, blockCounterStart));

        if(entry.isRateLimited()) throw new TooManyRequestsException("Rate limit reached", "RateLimitFilter.rateLimit");
    }

    ///..
    private void replenish() {

        final Iterator<Map.Entry<InetAddress, RateLimitEntry>> entries = tokensByIp.entrySet().iterator();

        while(entries.hasNext()) {

            final Map.Entry<InetAddress, RateLimitEntry> entry = entries.next();
            if(entry.getValue().doReplenish(maxTokensPerIp)) tokensByIp.remove(entry.getKey());
        }
    }

    ///
}
