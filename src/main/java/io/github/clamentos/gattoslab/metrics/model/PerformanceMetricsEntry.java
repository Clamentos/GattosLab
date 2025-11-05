package io.github.clamentos.gattoslab.metrics.model;

///
import io.github.clamentos.gattoslab.utils.Pair;

///.
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

///.
import lombok.Getter;

///
public final class PerformanceMetricsEntry {

    ///
    @Getter
    private final Map<String, PerformanceMetricsSubEntry> subEntries;

    ///..
    private final int rateCapacity;
    private final List<Pair<Integer, Integer>> latencyBuckets;

    ///
    public PerformanceMetricsEntry(final int rateCapacity, final List<Pair<Integer, Integer>> latencyBuckets) {

        subEntries = new ConcurrentHashMap<>();

        this.rateCapacity = rateCapacity;
        this.latencyBuckets = latencyBuckets;
    }

    ///..
    public void update(final String path, final int responseTime, final int index) {

        final PerformanceMetricsSubEntry subEntry = subEntries.computeIfAbsent(

            path,
            _ -> new PerformanceMetricsSubEntry(rateCapacity, latencyBuckets)
        );

        subEntry.getRequestCounters().get(index).incrementAndGet();

        for(int i = 0; i < latencyBuckets.size(); i++) {

            final LatencyBucket responseTimeRange = subEntry.getResponseTimeDistribution().get(i);

            if(responseTimeRange.isWithin(responseTime)) {

                responseTimeRange.incrementCount();
                break;
            }
        }
    }

    ///
}
