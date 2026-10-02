package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import io.github.clamentos.gattoslab.exchange.handling.components.Streamable;
import io.github.clamentos.gattoslab.http.server.StreamWriter;

///..
import java.io.IOException;
import java.util.Arrays;

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
        writer.write(Arrays.toString(this.labels));
        writer.write(",\"datasets\":[");

        final int length = this.datasets.length - 1;

        for(int i = 0; i < length; i++) {

            this.datasets[i].stream(writer);
            writer.write(",");
        }

        if(length >= 0) this.datasets[length].stream(writer);
        writer.write("]}");
    }

    ///
}
