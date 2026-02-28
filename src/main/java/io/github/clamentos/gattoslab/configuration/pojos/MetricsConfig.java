package io.github.clamentos.gattoslab.configuration.pojos;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class MetricsConfig {

    ///
    private final String dumpToDbSchedule;
    private final boolean enabled;
    private final int requestMetricsRetention;
    private final String retentionSchedule;
    private final int siphonCapacity;
    private final int systemMetricsRetention;
    private final String systemMetricsSampling;

    ///
}
