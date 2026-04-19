package io.github.clamentos.gattoslab.observability.metrics.entities.charts;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class ChartDataset<T> {

    ///
    private final String label;
    private final T data;

    ///
}
