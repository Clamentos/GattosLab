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
public final class RequestMetricsRateChart {

    ///
    private final List<Long> labels;
    private final List<ChartDataset<Integer>> datasets;

    ///
}
