package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import java.util.Set;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class PathInvocationAggregationEntity {

    ///
    private final String path;
    private final Set<Short> httpStatuses;
    private final long firstInvocation;
    private final long lastInvocation;
    private final long count;

    ///
}
