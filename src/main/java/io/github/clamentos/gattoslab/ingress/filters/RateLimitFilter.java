package io.github.clamentos.gattoslab.ingress.filters;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.pojos.RateLimitConfig;
import io.github.clamentos.gattoslab.exceptions.TooManyRequestsException;
import io.github.clamentos.gattoslab.observability.logging.SquashedLogContainer;
import io.github.clamentos.gattoslab.observability.logging.squash.SquashLogEventType;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.utils.GenericUtils;
import io.github.clamentos.gattoslab.utils.HttpUtils;

///..
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

///..
import java.net.InetAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

///..
import lombok.extern.slf4j.Slf4j;

///
@Slf4j

///
public final class RateLimitFilter {

    ///
    private final int maxTokensPerIp;
    private final int blockCounterStart;

    ///..
    private final SquashedLogContainer squashedLogContainer;

    ///..
    private final Map<String, RateLimitEntry> tokensByIp;

    ///
    public RateLimitFilter(final ApplicationProperties applicationProperties, final BatchScheduler batchScheduler, final SquashedLogContainer squashedLogContainer)
    throws IllegalArgumentException {

        final RateLimitConfig rateLimitConfig = applicationProperties.getRateLimitConfig();
        final int schedule = (int)batchScheduler.schedule(this::replenish, "RateLimitFilter::replenish", rateLimitConfig.getReplenishRate()).getPeriod();

        maxTokensPerIp = rateLimitConfig.getMaxTokensPerIp();
        blockCounterStart = rateLimitConfig.getRetryAfter() / schedule;

        this.squashedLogContainer = squashedLogContainer;

        tokensByIp = new ConcurrentHashMap<>();
    }

    ///
    public void rateLimit(final HttpServerExchange exchange) throws TooManyRequestsException {

        final InetAddress ip = exchange.getSourceAddress().getAddress();
        final RateLimitEntry entry = tokensByIp.computeIfAbsent(ip.getHostAddress(), _ -> new RateLimitEntry(maxTokensPerIp, blockCounterStart));

        if(entry.isRateLimited()) {

            final String fingerprint = GenericUtils.composeFingerprint(ip, HttpUtils.getHeaderValue(exchange.getRequestHeaders(), Headers.USER_AGENT_STRING));
            squashedLogContainer.squash(SquashLogEventType.RATE_LIMIT, fingerprint);

            throw new TooManyRequestsException("Rate limit reached for fingerprint: " + fingerprint);
        }
    }

    ///.
    private void replenish() {

        final Iterator<Map.Entry<String, RateLimitEntry>> entries = tokensByIp.entrySet().iterator();

        while(entries.hasNext()) {

            final Map.Entry<String, RateLimitEntry> entry = entries.next();
            if(entry.getValue().doReplenish(maxTokensPerIp)) tokensByIp.remove(entry.getKey());
        }
    }

    ///
}
