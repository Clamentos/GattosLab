package io.github.clamentos.gattoslab.observability.metrics.entities.charts;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class RequestMetricsCharts {

    ///
    private final RequestMetricsRateChart rate;
    private final RequestMetricsLatencyChart latency;

    ///
}
