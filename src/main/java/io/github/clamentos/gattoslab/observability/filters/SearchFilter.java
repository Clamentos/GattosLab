package io.github.clamentos.gattoslab.observability.filters;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///..
import lombok.Getter;

///
@Getter

///
public class SearchFilter {

    ///
    private final long startTimestamp;
    private final long endTimestamp;

    ///
    @JsonCreator
    public SearchFilter(@JsonProperty("startTimestamp") final long startTimestamp, @JsonProperty("endTimestamp") final long endTimestamp) {

        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
    }

    ///
}
