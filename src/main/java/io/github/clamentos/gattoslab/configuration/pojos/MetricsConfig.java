package io.github.clamentos.gattoslab.configuration.pojos;

///
import java.time.Duration;

///..
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
    private final Duration requestMetricsRetention;
    private final String retentionSchedule;
    private final int siphonCapacity;
    private final Duration systemMetricsRetention;
    private final String systemMetricsSampling;

    ///
}
