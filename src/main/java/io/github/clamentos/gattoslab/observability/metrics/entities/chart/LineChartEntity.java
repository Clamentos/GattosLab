package io.github.clamentos.gattoslab.observability.metrics.entities.chart;

///
import io.github.clamentos.gattoslab.datastructures.StreamWriter;
import io.github.clamentos.gattoslab.datastructures.interfaces.Streamable;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.IOException;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class LineChartEntity implements Streamable {

    ///
    private final long[] labels;
    private final LineChartEntry[] datasets;

    ///
    @Override
    public void stream(final StreamWriter writer) throws IOException {

        writer.write("{\"labels\":");
        GenericUtils.longArrayToString(this.labels, writer);
        writer.write(",\"datasets\":[");

        final int length = this.datasets.length - 1;

        for(int i = 0; i < length; i++) {

            this.datasets[i].stream(writer);
            writer.write(',');
        }

        if(length >= 0) this.datasets[length].stream(writer);
        writer.write("]}");
    }

    ///
}
