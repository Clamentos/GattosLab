package io.github.clamentos.gattoslab.observability.filters;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///..
import java.util.Set;

///..
import lombok.Getter;

///
@Getter

///
public final class RequestMetricsSearchFilter extends AggregatedSearchFilter {

    ///
    private final Boolean onlyOthers;
    private final String pathPattern;
    private final Set<Integer> httpStatuses;
    private final String userAgentPattern;

    ///
    @JsonCreator
    public RequestMetricsSearchFilter(

        @JsonProperty("startTimestamp") final long startTimestamp,
        @JsonProperty("endTimestamp") final long endTimestamp,
        @JsonProperty("bucketSize") final Long bucketSize,
        @JsonProperty("onlyOthers") final Boolean onlyOthers,
        @JsonProperty("pathPattern") final String pathPattern,
        @JsonProperty("httpStatuses") final Set<Integer> httpStatuses,
        @JsonProperty("userAgentPattern") final String userAgentPattern
    ) {

        super(startTimestamp, endTimestamp, bucketSize);

        this.onlyOthers = onlyOthers;
        this.pathPattern = pathPattern;
        this.httpStatuses = httpStatuses;
        this.userAgentPattern = userAgentPattern;
    }

    ///
}
