package io.github.clamentos.gattoslab.ratelimiting;

///
import io.github.clamentos.gattoslab.exceptions.TooManyRequestsException;

///.
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

///.
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

///
@Component

///
public class RateLimiter implements HandlerInterceptor {

    ///
    private final Map<String, AtomicInteger> counters;
    private final int maxTokens;

    ///
    @Autowired
    public RateLimiter(final Environment environment) {

        counters = new ConcurrentHashMap<>();
        maxTokens = environment.getProperty("app.ratelimit.maxTokens", Integer.class, 1000);
    }

    ///
    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler)
    throws TooManyRequestsException {

        final String ip = request.getRemoteAddr();
        final boolean tooManyRequests = counters.computeIfAbsent(ip, _ -> new AtomicInteger(maxTokens)).decrementAndGet() < 0;

        if(tooManyRequests) throw new TooManyRequestsException(ip);

		return true;
	}

    ///.
    @Scheduled(fixedRateString = "${app.ratelimit.replenishDelay}")
    protected void replenish() {

        for(final Map.Entry<String, AtomicInteger> entry : counters.entrySet()) {

            final AtomicInteger counter = entry.getValue();
            final int counterValue = counter.get();

            if(counterValue < 0 || counterValue == maxTokens) counters.remove(entry.getKey());
            else counter.set(maxTokens);
        }
    }

    ///
}
