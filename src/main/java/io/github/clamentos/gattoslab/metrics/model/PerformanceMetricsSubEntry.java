package io.github.clamentos.gattoslab.metrics.model;

///
import io.github.clamentos.gattoslab.utils.Pair;

///.
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

///.
import lombok.Getter;

///
@Getter

///
public final class PerformanceMetricsSubEntry {

    ///
    private final List<LatencyBucket> responseTimeDistribution;
    private final List<AtomicLong> requestCounters;

    ///
    public PerformanceMetricsSubEntry(final int rateCapacity, final List<Pair<Integer, Integer>> latencyBuckets) {

        responseTimeDistribution = latencyBuckets.stream().map(LatencyBucket::new).toList();
        requestCounters = new ArrayList<>(rateCapacity);

        for(int i = 0; i < rateCapacity; i++) requestCounters.add(new AtomicLong());
    }

    ///
}
