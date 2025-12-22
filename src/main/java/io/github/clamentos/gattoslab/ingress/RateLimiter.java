package io.github.clamentos.gattoslab.ingress;

///
import io.github.clamentos.gattoslab.configuration.PropertyProvider;
import io.github.clamentos.gattoslab.exceptions.TooManyRequestsException;
import io.github.clamentos.gattoslab.observability.logging.SquashedLogContainer;
import io.github.clamentos.gattoslab.utils.Pair;

///.
import jakarta.el.PropertyNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

///.
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

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
    private final Map<String, Pair<AtomicInteger, AtomicInteger>> tokensByIp;

    ///
    @Autowired
    public RateLimiter(final PropertyProvider propertyProvider, final SquashedLogContainer squashedLogContainer) throws PropertyNotFoundException {

        maxTokensPerIp = propertyProvider.getProperty("app.ratelimit.maxTokensPerIp", Integer.class);

        final int retryAfter = propertyProvider.getProperty("app.ratelimit.retryAfter", Integer.class);
        blockCounterStart = retryAfter / propertyProvider.getProperty("app.ratelimit.replenishRate", Integer.class);

        this.squashedLogContainer = squashedLogContainer;

        tokensByIp = new ConcurrentHashMap<>();
    }

    ///
    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler)
    throws TooManyRequestsException {

        final String ip = request.getRemoteAddr();

        request.setAttribute("IP_ATTRIBUTE", ip);
        request.setAttribute("REQUEST_METHOD", request.getMethod());
        request.setAttribute("START_TIME", System.currentTimeMillis());

        final Pair<AtomicInteger, AtomicInteger> entry = tokensByIp.computeIfAbsent(ip, _ -> new Pair<>(

            new AtomicInteger(maxTokensPerIp),
            new AtomicInteger(blockCounterStart)
        ));

        if(entry.getA().getAndDecrement() <= 0) this.tooManyRequests(ip, "Rate limit reached for ip: " + ip);
		return true;
	}

    ///.
    @Scheduled(fixedRateString = "${app.ratelimit.replenishRate}", scheduler = "batchScheduler")
    protected void replenish() {

        final Iterator<Map.Entry<String, Pair<AtomicInteger, AtomicInteger>>> entries = tokensByIp.entrySet().iterator();

        while(entries.hasNext()) {

            final Map.Entry<String, Pair<AtomicInteger, AtomicInteger>> entry = entries.next();

            final AtomicInteger tokenCounter = entry.getValue().getA();
            final AtomicInteger blockCounter = entry.getValue().getB();
            final int tokenCounterValue = tokenCounter.get();
            final int blockCounterValue = blockCounter.get();

            if(tokenCounterValue == maxTokensPerIp || (tokenCounterValue <= 0 && blockCounterValue == 0)) tokensByIp.remove(entry.getKey());
            else if(tokenCounterValue <= 0 && blockCounterValue > 0) blockCounter.decrementAndGet();
            else tokenCounter.set(maxTokensPerIp);
        }
    }

    ///.
    private void tooManyRequests(final String key, final String message) throws TooManyRequestsException {

        squashedLogContainer.squashRateLimitLog(key);
        throw new TooManyRequestsException(message);
    }

    ///
}
