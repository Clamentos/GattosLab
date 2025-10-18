package io.github.clamentos.gattoslab.metrics;

///
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

///.
import lombok.Getter;

///.
import org.springframework.http.HttpStatus;

///
@Getter

///
public final class MetricsEntry {

    ///
    private final Map<HttpStatus, List<AtomicInteger>> requestCounts;
    private final Map<HttpStatus, List<LatencyRange>> latencies;

    ///..
    private final int rateCapacity;
    private final int numLatencyBuckets;
    private final int bucketSize;

    ///
    public MetricsEntry(final int rateCapacity, final int numLatencyBuckets, final int maxLatencyBucket) {

        requestCounts = new ConcurrentHashMap<>();
        latencies = new ConcurrentHashMap<>();

        this.rateCapacity = rateCapacity;
        this.numLatencyBuckets = numLatencyBuckets;
        this.bucketSize = maxLatencyBucket / numLatencyBuckets;
    }

    ///..
    public void update(final HttpStatus status, final int elapsedTime, final int index) {

        final List<AtomicInteger> counts = requestCounts.computeIfAbsent(status, _ -> {

            final List<AtomicInteger> buckets = new ArrayList<>(rateCapacity);
            for(int i = 0; i < rateCapacity; i++) buckets.add(new AtomicInteger());

            return buckets;
        });

        counts.get(index).incrementAndGet();

        final List<LatencyRange> ranges = latencies.computeIfAbsent(status, _ -> {

            final List<LatencyRange> buckets = new ArrayList<>(numLatencyBuckets);
            int counter = 0;

            for(int i = 0; i < numLatencyBuckets; i++) {

                buckets.add(new LatencyRange(counter, counter + bucketSize - 1));
                counter += bucketSize;
            }

            return buckets;
        });

        for(final LatencyRange range : ranges) {

            if(range.isWithin(elapsedTime)) range.incrementCount();
        }
    }

    ///
}
