package io.github.clamentos.gattoslab.observability.metrics.entities;

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
