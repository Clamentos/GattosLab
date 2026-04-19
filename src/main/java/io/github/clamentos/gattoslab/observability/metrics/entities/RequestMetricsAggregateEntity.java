package io.github.clamentos.gattoslab.observability.metrics.entities;

///.
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class RequestMetricsAggregateEntity {

    ///
    private final String key;
    private final long timeSlot;
    private final boolean isOthers;
    private final int rate;
    private final int[] latencyDistribution;

    ///
}
