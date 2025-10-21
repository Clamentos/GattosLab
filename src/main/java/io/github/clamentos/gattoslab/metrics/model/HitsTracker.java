package io.github.clamentos.gattoslab.metrics.model;

///
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

///.
import lombok.Getter;

///
@Getter

///
public final class HitsTracker {

    ///
    private final Map<String, AtomicLong> totalRequestCounter;
    private final Map<String, AtomicLong> userAgentCounter;

    ///..
    private final AtomicInteger totalRequestCounterSize;
    private final AtomicInteger totalUserAgentCounterSize;

    ///..
    private final int maxTotalRequestCounterSize;
    private final int maxUserAgentCounterSize;

    ///
    public HitsTracker(final int maxTotalRequestCounterSize, int maxUserAgentCounterSize) {

        this.totalRequestCounter = new ConcurrentHashMap<>();
        this.userAgentCounter = new ConcurrentHashMap<>();

        totalRequestCounterSize = new AtomicInteger();
        totalUserAgentCounterSize = new AtomicInteger();

        this.maxTotalRequestCounterSize = maxTotalRequestCounterSize;
        this.maxUserAgentCounterSize = maxUserAgentCounterSize;
    }

    ///
    public void updateRequestCount(final String path) {

        if(totalRequestCounterSize.get() < maxTotalRequestCounterSize) {

            totalRequestCounter.computeIfAbsent(path, _ -> {

                totalRequestCounterSize.incrementAndGet();
                return new AtomicLong();

            }).incrementAndGet();
        }

        else {

            final AtomicLong counter = totalRequestCounter.get(path);
            if(counter != null) counter.incrementAndGet();
        }
    }

    ///..
    public void updateUserAgentCount(final String userAgent) {

        if(totalUserAgentCounterSize.get() < maxUserAgentCounterSize) {

            userAgentCounter.computeIfAbsent(userAgent, _ -> {

                totalUserAgentCounterSize.incrementAndGet();
                return new AtomicLong();

            }).incrementAndGet();
        }

        else {

            final AtomicLong counter = userAgentCounter.get(userAgent);
            if(counter != null) counter.incrementAndGet();
        }
    }

    ///
}
