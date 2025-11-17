package io.github.clamentos.gattoslab.observability.filters;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///..
import com.mongodb.client.model.Filters;

///.
import lombok.Getter;

///.
import org.bson.conversions.Bson;

///
@Getter

///
public class TemporalSearchFilter implements SearchFilter {

    ///
    private final long startTimestamp;
    private final long endTimestamp;

    ///
    @JsonCreator
    public TemporalSearchFilter(

        @JsonProperty("startTimestamp") final long startTimestamp,
        @JsonProperty("endTimestamp") final long endTimestamp
    ) {

        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
    }

    ///
    @Override
    public Bson toBsonFilter() {

        return Filters.and(Filters.gte("timestamp", startTimestamp), Filters.lte("timestamp", endTimestamp));
    }

    ///
}
