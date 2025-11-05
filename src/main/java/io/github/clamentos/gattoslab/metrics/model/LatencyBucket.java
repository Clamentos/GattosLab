package io.github.clamentos.gattoslab.metrics.model;

///
import io.github.clamentos.gattoslab.utils.Pair;

///.
import java.util.concurrent.atomic.AtomicLong;

///.
import lombok.Getter;

///
@Getter

///
public final class LatencyBucket {

    ///
    private final int start;
    private final int end;
    private final AtomicLong count;

    ///
    public LatencyBucket(final Pair<Integer, Integer> range) {

        start = range.getA();
        end = range.getB();
        count = new AtomicLong();
    }

    ///
    public boolean isWithin(final int value) {

        return value >= start && value <= end;
    }

    ///..
    public void incrementCount() {

        count.incrementAndGet();
    }

    ///
}
