package io.github.clamentos.gattoslab.ingress.ratelimit;

///
import io.github.clamentos.gattoslab.configuration.PropertyProvider;
import io.github.clamentos.gattoslab.exceptions.TooManyRequestsException;
import io.github.clamentos.gattoslab.observability.logging.SquashedLogContainer;
import io.github.clamentos.gattoslab.observability.logging.log_squash.SquashLogEventType;

///.
import jakarta.el.PropertyNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

///.
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

///..
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

///
@Component
@Slf4j

///
public final class RateLimiter implements HandlerInterceptor {

    ///
    private final int maxTokensPerIp;
    private final int blockCounterStart;

    ///..
    private final SquashedLogContainer squashedLogContainer;

    ///..
    private final Map<String, RateLimitEntry> tokensByIp;

    ///
    @Autowired
    public RateLimiter(@NonNull final PropertyProvider propertyProvider, @NonNull final SquashedLogContainer squashedLogContainer)
    throws PropertyNotFoundException {

        maxTokensPerIp = propertyProvider.getProperty("app.ratelimit.maxTokensPerIp", Integer.class);

        final int retryAfter = propertyProvider.getProperty("app.ratelimit.retryAfter", Integer.class);
        blockCounterStart = retryAfter / propertyProvider.getProperty("app.ratelimit.replenishRate", Integer.class);

        this.squashedLogContainer = squashedLogContainer;

        tokensByIp = new ConcurrentHashMap<>();
    }

    ///
    @Override
    public boolean preHandle(@NonNull final HttpServletRequest request, @Nullable final HttpServletResponse response, @Nullable final Object handler)
    throws TooManyRequestsException {

        final String ip = request.getRemoteAddr();
        final RateLimitEntry entry = tokensByIp.computeIfAbsent(ip, _ -> new RateLimitEntry(maxTokensPerIp, blockCounterStart));

        if(entry.isRateLimited()) {

            squashedLogContainer.squash(SquashLogEventType.RATE_LIMIT, ip);
            throw new TooManyRequestsException("Rate limit reached for ip: " + ip);
        }

		return true;
	}

    ///.
    @Scheduled(fixedRateString = "${app.ratelimit.replenishRate}", scheduler = "batchScheduler")
    protected void replenish() {

        final Iterator<Map.Entry<String, RateLimitEntry>> entries = tokensByIp.entrySet().iterator();

        while(entries.hasNext()) {

            final Map.Entry<String, RateLimitEntry> entry = entries.next();
            if(entry.getValue().doReplenish(maxTokensPerIp)) tokensByIp.remove(entry.getKey());
        }
    }

    ///
}
