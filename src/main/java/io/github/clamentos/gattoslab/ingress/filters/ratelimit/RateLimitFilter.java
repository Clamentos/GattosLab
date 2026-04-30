package io.github.clamentos.gattoslab.ingress.filters.ratelimit;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exceptions.TooManyRequestsException;
import io.github.clamentos.gattoslab.http.HttpUtils;
import io.github.clamentos.gattoslab.observability.logging.SquashedLogsContainer;
import io.github.clamentos.gattoslab.observability.logging.squash.SquashLogEventType;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.utils.GenericUtils;

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
    private final SquashedLogsContainer squashedLogsContainer;

    ///..
    private final Map<InetAddress, RateLimitEntry> tokensByIp;

    ///
    public RateLimitFilter(final ApplicationProperties applicationProperties, final BatchScheduler batchScheduler, final SquashedLogsContainer squashedLogsContainer)
    throws IllegalArgumentException {

        final int schedule = (int)batchScheduler.schedule(this::replenish, "RateLimitFilter::replenish", applicationProperties.getRateLimitReplenishRate());

        maxTokensPerIp = applicationProperties.getRateLimitMaxTokensPerIp();
        blockCounterStart = (int)(applicationProperties.getRateLimitRetryAfter().toMillis() / schedule);

        this.squashedLogsContainer = squashedLogsContainer;

        tokensByIp = new ConcurrentHashMap<>();
    }

    ///
    public void rateLimit(final HttpServerExchange exchange) throws TooManyRequestsException {

        final InetAddress ip = exchange.getSourceAddress().getAddress();
        final RateLimitEntry entry = tokensByIp.computeIfAbsent(ip, _ -> new RateLimitEntry(maxTokensPerIp, blockCounterStart));

        if(entry.isRateLimited()) {

            final String fingerprint = GenericUtils.composeFingerprint(ip, HttpUtils.getHeaderValue(exchange.getRequestHeaders(), Headers.USER_AGENT_STRING));
            squashedLogsContainer.squash(SquashLogEventType.RATE_LIMIT, fingerprint);

            throw new TooManyRequestsException("RateLimitFilter.rateLimit~Rate limit reached for fingerprint: " + fingerprint);
        }
    }

    ///.
    private void replenish() {

        final Iterator<Map.Entry<InetAddress, RateLimitEntry>> entries = tokensByIp.entrySet().iterator();

        while(entries.hasNext()) {

            final Map.Entry<InetAddress, RateLimitEntry> entry = entries.next();
            if(entry.getValue().doReplenish(maxTokensPerIp)) tokensByIp.remove(entry.getKey());
        }
    }

    ///
}
