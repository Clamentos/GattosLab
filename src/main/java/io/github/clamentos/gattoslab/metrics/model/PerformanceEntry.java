package io.github.clamentos.gattoslab.metrics.model;

///
import io.github.clamentos.gattoslab.utils.Pair;

///.
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

///.
import lombok.Getter;

///.
import org.springframework.http.HttpStatus;

///
public final class PerformanceEntry {

    ///
    @Getter
    private final Map<HttpStatus, PerformanceSubEntry> subEntries;

    ///..
    private final int rateCapacity;
    private final List<Pair<Integer, Integer>> latencyBuckets;

    ///
    public PerformanceEntry(final int rateCapacity, final List<Pair<Integer, Integer>> latencyBuckets) {

        subEntries = new ConcurrentHashMap<>();

        this.rateCapacity = rateCapacity;
        this.latencyBuckets = latencyBuckets;
    }

    ///..
    public void update(final HttpStatus status, final int responseTime, final int index) {

        final PerformanceSubEntry subEntry = subEntries.computeIfAbsent(status, _ -> new PerformanceSubEntry(rateCapacity, latencyBuckets));
        subEntry.getRequestCounter().get(index).incrementAndGet();

        for(int i = 0; i < latencyBuckets.size(); i++) {

            final LatencyRange responseTimeRange = subEntry.getResponseTimeDistribution().get(i);
            if(responseTimeRange.isWithin(responseTime)) responseTimeRange.incrementCount();
        }
    }

    ///
}
