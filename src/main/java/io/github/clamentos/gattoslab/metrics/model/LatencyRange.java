package io.github.clamentos.gattoslab.metrics.model;

///
import java.util.concurrent.atomic.AtomicLong;

///.
import lombok.Getter;

///
@Getter

///
public final class LatencyRange {

    ///
    private final int start;
    private final int end;
    private final AtomicLong count;

    ///
    public LatencyRange(final int start, final int end) {

        this.start = start;
        this.end = end;
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
