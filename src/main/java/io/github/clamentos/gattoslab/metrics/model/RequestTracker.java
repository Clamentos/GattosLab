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
public final class RequestTracker {

    ///
    private final Map<String, AtomicLong> pathCounters;
    private final Map<String, AtomicLong> userAgentCounters;

    ///..
    private final AtomicInteger pathCountersSize;
    private final AtomicInteger userAgentCountersSize;

    ///..
    private final int maxPathCountersSize;
    private final int maxUserAgentCountersSize;

    ///
    public RequestTracker(final int maxPathCountersSize, final int maxUserAgentCountersSize) {

        this.pathCounters = new ConcurrentHashMap<>();
        this.userAgentCounters = new ConcurrentHashMap<>();

        pathCountersSize = new AtomicInteger();
        userAgentCountersSize = new AtomicInteger();

        this.maxPathCountersSize = maxPathCountersSize;
        this.maxUserAgentCountersSize = maxUserAgentCountersSize;
    }

    ///
    public void updateRequestCount(final String path) {

        this.update(path, pathCounters, pathCountersSize, maxPathCountersSize);
    }

    ///..
    public void updateUserAgentCount(final String userAgent) {

        this.update(userAgent, userAgentCounters, userAgentCountersSize, maxUserAgentCountersSize);
    }

    ///.
    private void update(final String key, final Map<String, AtomicLong> counters, final AtomicInteger sizeCounter, final int cap) {

        if(sizeCounter.get() <= cap) {

            counters.computeIfAbsent(key, _ -> {

                sizeCounter.incrementAndGet();
                return new AtomicLong();

            }).incrementAndGet();
        }

        else {

            final AtomicLong counter = counters.get(key);
            if(counter != null) counter.incrementAndGet();
        }
    }

    ///
}
