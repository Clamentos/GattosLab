package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import io.github.clamentos.gattoslab.exchange.handling.components.Streamable;
import io.github.clamentos.gattoslab.http.server.StreamWriter;

///..
import java.io.IOException;
import java.util.Arrays;

///..
import lombok.Getter;

///
@Getter

///
public final class LineChartEntry implements Streamable {

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
    public void stream(final StreamWriter writer) throws IOException {

        writer.write("{\"label\":\"");
        writer.write(this.label);
        writer.write("\",\"data\":");
        writer.write(Arrays.toString(this.data));
        writer.write("}");
    }

    ///
}
