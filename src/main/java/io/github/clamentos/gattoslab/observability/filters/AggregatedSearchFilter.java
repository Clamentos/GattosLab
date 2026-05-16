package io.github.clamentos.gattoslab.observability.filters;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///..
import lombok.Getter;

///
@Getter

///
public class AggregatedSearchFilter extends SearchFilter {

    ///
    private final Long bucketSize;

    ///
    @JsonCreator
    public AggregatedSearchFilter(

        @JsonProperty("startTimestamp") final long startTimestamp,
        @JsonProperty("endTimestamp") final long endTimestamp,
        @JsonProperty("bucketSize") final Long bucketSize
    ) {

        super(startTimestamp, endTimestamp);
        this.bucketSize = bucketSize;
    }

    ///
}
