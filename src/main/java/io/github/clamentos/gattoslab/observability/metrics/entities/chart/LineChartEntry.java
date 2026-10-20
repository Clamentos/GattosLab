package io.github.clamentos.gattoslab.observability.metrics.entities.chart;

///
import io.github.clamentos.gattoslab.datastructures.StreamWriter;
import io.github.clamentos.gattoslab.datastructures.interfaces.Streamable;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.IOException;

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
        GenericUtils.longArrayToString(this.data, writer);
        writer.write('}');
    }

    ///
}
