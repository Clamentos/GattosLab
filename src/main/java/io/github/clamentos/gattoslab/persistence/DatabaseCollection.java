package io.github.clamentos.gattoslab.persistence;

///
import io.github.clamentos.gattoslab.configuration.dynamic.DynamicPropertyEntity;
import io.github.clamentos.gattoslab.observability.logging.entities.LogEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.RequestMetricsEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.SystemMetricsEntity;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public enum DatabaseCollection {

    ///
    LOGS("Logs", LogEntity.class),
    REQUEST_METRICS("RequestMetrics", RequestMetricsEntity.class),
    SYSTEM_METRICS("SystemMetrics", SystemMetricsEntity.class),
    PROPERTIES("Properties", DynamicPropertyEntity.class);

    ///
    private final String value;
    private final Class<?> entityClass;

    ///
}
