package io.github.clamentos.gattoslab.observability.logging.log_squash;

///
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.stereotype.Component;

///
@Component
@Slf4j

///
public final class RateLimitSquash implements SquashLogEvent {

    ///
    private final Map<String, AtomicInteger> rateLimitSquashes;

    ///
    public RateLimitSquash() {

        rateLimitSquashes = new ConcurrentHashMap<>();
    }

    ///
    @Override
    public SquashLogEventType getType() {

        return SquashLogEventType.RATE_LIMIT;
    }

    ///..
    @Override
    public void update(Object value) {

        rateLimitSquashes.computeIfAbsent(value.toString(), _ -> new AtomicInteger()).incrementAndGet();
    }

    ///..
    @Override
    public void log() {

        for(final Map.Entry<String, AtomicInteger> rateLimitSquashEntry : rateLimitSquashes.entrySet()) {

            log.warn("Rate limit reached {} times for ip: {}", rateLimitSquashEntry.getValue(), rateLimitSquashEntry.getKey());
        }
    }

    ///..
    @Override
    public void reset() {

        rateLimitSquashes.clear();
    }

    ///
}
