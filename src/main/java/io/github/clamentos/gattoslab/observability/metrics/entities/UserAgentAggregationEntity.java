package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class UserAgentAggregationEntity {

    ///
    private final String userAgent;
    private final long firstInvocation;
    private final long lastInvocation;
    private final long count;

    ///
}
