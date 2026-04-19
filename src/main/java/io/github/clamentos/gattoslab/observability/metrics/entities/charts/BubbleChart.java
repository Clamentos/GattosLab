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
public final class BubbleChart {

    ///
    private final List<ChartDataset<List<BubbleChartDataEntry>>> datasets;

    ///
}
