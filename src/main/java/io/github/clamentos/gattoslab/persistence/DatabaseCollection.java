package io.github.clamentos.gattoslab.persistence;

///
import io.github.clamentos.gattoslab.observability.logging.LogEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.RequestMetricsEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.SystemMetricsEntity;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///..
import org.bson.Document;

///
@AllArgsConstructor
@Getter

///
public enum DatabaseCollection {

    ///
    LOGS("Logs", LogEntity.class),
    REQUEST_METRICS("RequestMetrics", RequestMetricsEntity.class),
    SYSTEM_METRICS("SystemMetrics", SystemMetricsEntity.class),
    PROPERTIES("Properties", Document.class);

    ///
    private final String value;
    private final Class<?> entityClass;

    ///
}
