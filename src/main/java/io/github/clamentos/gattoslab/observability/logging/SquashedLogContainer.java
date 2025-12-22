package io.github.clamentos.gattoslab.observability.logging;

///
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

///
@Component
@Slf4j

///
public final class SquashedLogContainer {

    ///
    private final Map<String, AtomicInteger> rateLimitSquashMap;
    private final AtomicInteger ifModifiedSinceMalformationsCounter;

    ///
    public SquashedLogContainer() {

        rateLimitSquashMap = new ConcurrentHashMap<>();
        ifModifiedSinceMalformationsCounter = new AtomicInteger();
    }

    ///
    public void squashRateLimitLog(final String key) {

        rateLimitSquashMap.computeIfAbsent(key, _ -> new AtomicInteger()).incrementAndGet();
    }

    ///..
    public void squashIfModifiedSinceParseLog() {

        ifModifiedSinceMalformationsCounter.incrementAndGet();
    }

    ///.
    @Scheduled(cron = "${app.squash.logSchedule}", scheduler = "batchScheduler")
    protected void log() {

        final int counterValue = ifModifiedSinceMalformationsCounter.getAndSet(0);
        if(counterValue > 0) log.warn("Date-time parse exception for header If-Modified-Since: {} times", counterValue);

        for(final Map.Entry<String, AtomicInteger> rateLimitSquashEntry : rateLimitSquashMap.entrySet()) {

            log.warn("Rate limit reached {} times for ip: {}", rateLimitSquashEntry.getValue(), rateLimitSquashEntry.getKey());
        }

        rateLimitSquashMap.clear();
    }

    ///
}
