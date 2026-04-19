package io.github.clamentos.gattoslab.observability.metrics.entities.charts;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class SystemMetricsCharts {

    ///
    private final LineChart threads;
    private final LineChart classes;
    private final LineChart ioResources;
    private final LineChart gcs;
    private final LineChart cpu;
    private final LineChart memory;
    private final LineChart storage;

    ///
}
