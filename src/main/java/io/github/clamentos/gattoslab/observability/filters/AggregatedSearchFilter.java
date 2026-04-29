package io.github.clamentos.gattoslab.observability.filters;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///..
import io.github.clamentos.gattoslab.persistence.EntityField;

///..
import lombok.Getter;

///
@Getter

///
public class AggregatedSearchFilter extends TemporalSearchFilter {

    ///
    private final Long bucketSize;

    ///
    @JsonCreator
    public AggregatedSearchFilter(

        @JsonProperty(EntityField.START_TIMESTAMP) final long startTimestamp,
        @JsonProperty(EntityField.END_TIMESTAMP) final long endTimestamp,
        @JsonProperty(EntityField.BUCKET_SIZE) final Long bucketSize
    ) {

        super(startTimestamp, endTimestamp);
        this.bucketSize = bucketSize;
    }

    ///
}
