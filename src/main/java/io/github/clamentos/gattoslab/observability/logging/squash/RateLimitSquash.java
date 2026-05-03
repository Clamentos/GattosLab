package io.github.clamentos.gattoslab.observability.logging.squash;

///
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

///..
import lombok.extern.slf4j.Slf4j;

///
@Slf4j

///
public class RateLimitSquash implements SquashLogEvent {

    ///
    protected final Map<String, AtomicInteger> squashesMap;

    ///
    public RateLimitSquash() {

        squashesMap = new ConcurrentHashMap<>();
    }

    ///
    @Override
    public SquashLogEventType getType() {

        return SquashLogEventType.RATE_LIMIT;
    }

    ///..
    @Override
    public void update(final Object value) {

        if(value != null) squashesMap.computeIfAbsent(value.toString(), _ -> new AtomicInteger()).incrementAndGet();
    }

    ///..
    @Override
    public void log() {

        for(final Map.Entry<String, AtomicInteger> rateLimitSquashEntry : squashesMap.entrySet()) {

            log.warn("Rate limit reached {} times for fingerprint: {}", rateLimitSquashEntry.getValue(), rateLimitSquashEntry.getKey());
        }
    }

    ///..
    @Override
    public void reset() {

        squashesMap.clear();
    }

    ///
}
