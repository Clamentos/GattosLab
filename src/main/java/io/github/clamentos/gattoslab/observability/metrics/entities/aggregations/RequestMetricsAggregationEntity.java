package io.github.clamentos.gattoslab.observability.metrics.entities.aggregations;

///
import lombok.Getter;
import lombok.Setter;

///
@Getter
@Setter

///
public final class RequestMetricsAggregationEntity {

    ///
    private int count;
    private long latencySum;

    ///
}
