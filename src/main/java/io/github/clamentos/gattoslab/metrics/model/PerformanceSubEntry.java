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
public final class PerformanceSubEntry {

    ///
    private final List<AtomicLong> requestCounter;
    private final List<LatencyRange> responseTimeDistribution;

    ///
    public PerformanceSubEntry(final int rateCapacity, final List<Pair<Integer, Integer>> latencyBuckets) {

        requestCounter = new ArrayList<>(rateCapacity);
        for(int i = 0; i < rateCapacity; i++) requestCounter.add(new AtomicLong());

        responseTimeDistribution = latencyBuckets.stream().map(element -> new LatencyRange(element.getA(), element.getB())).toList();
    }

    ///
}
