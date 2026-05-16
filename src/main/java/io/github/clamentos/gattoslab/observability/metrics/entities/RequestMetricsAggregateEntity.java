package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import lombok.Getter;

///
@Getter

///
public final class RequestMetricsAggregateEntity {

    ///
    private int rate;
    private long latencySum;

    ///..
    public void update(final RequestMetricsEntity entity) {

        rate++;
        latencySum += entity.getLatency();
    }

    ///
}
