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
public final class RequestMetricsLatencyChart {

    ///
    private final List<ChartDataset<BubbleChartDataEntry>> datasets;

    ///
}
