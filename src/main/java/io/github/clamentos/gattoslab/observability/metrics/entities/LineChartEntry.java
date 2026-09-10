package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import io.github.clamentos.gattoslab.datastructures.FastAsciiJoiner;
import io.github.clamentos.gattoslab.observability.Printable;

///..
import java.util.Arrays;

///..
import lombok.Getter;

///
@Getter

///
public final class LineChartEntry implements Printable {

    ///
    private final String label;
    private final long[] data;

    ///
    public LineChartEntry(final String label, final int dataLength) {

        this.label = label;
        this.data = new long[dataLength];
    }

    ///
    @Override
    public void appendBytes(final FastAsciiJoiner joiner) {

        joiner.add("{\"label\":\"");
        joiner.add(this.label);
        joiner.add("\",\"data\":");
        joiner.add(Arrays.toString(this.data));
        joiner.add("}");
    }

    ///
}
