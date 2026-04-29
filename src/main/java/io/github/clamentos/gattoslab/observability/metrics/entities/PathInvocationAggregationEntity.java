package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class PathInvocationAggregationEntity {

    ///
    private final String path;
    private final long firstInvocation;
    private final long lastInvocation;
    private final long count;
    private final boolean isOthers;
    private final short[] httpStatuses;

    ///
}
