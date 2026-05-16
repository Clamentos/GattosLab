package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import com.fasterxml.jackson.annotation.JsonProperty;

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
    private final long firstInvocation;
    private final long lastInvocation;
    private final int count;

    @Getter(onMethod = @__({@JsonProperty("isOthers")}))
    private final boolean isOthers;

    private final int[] httpStatuses;

    ///
}
