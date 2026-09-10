package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import io.github.clamentos.gattoslab.datastructures.FastAsciiJoiner;
import io.github.clamentos.gattoslab.observability.Printable;

///..
import java.util.Arrays;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class LineChartEntity implements Printable {

    ///
    private final long[] labels;
    private final LineChartEntry[] datasets;

    ///
    @Override
    public void appendBytes(final FastAsciiJoiner joiner) {

        joiner.add("{\"labels\":");
        joiner.add(Arrays.toString(this.labels));
        joiner.add(",\"datasets\":[");

            for(final LineChartEntry dataset : this.datasets) {

                dataset.appendBytes(joiner);
                joiner.add(",");
            }

        joiner.replaceLast("]}");
    }

    ///
}
