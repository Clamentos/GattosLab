package io.github.clamentos.gattoslab.observability.metrics.entities.charts;

///
import java.util.List;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class ChartDataset<T> {

    ///
    private final String label;
    private final List<T> data;

    ///
}
