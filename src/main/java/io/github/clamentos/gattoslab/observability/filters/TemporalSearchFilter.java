package io.github.clamentos.gattoslab.observability.filters;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///..
import com.mongodb.client.model.Filters;

///..
import io.github.clamentos.gattoslab.persistence.EntityField;

///..
import lombok.Getter;

///..
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
    public TemporalSearchFilter(@JsonProperty(EntityField.START_TIMESTAMP) final long startTimestamp, @JsonProperty(EntityField.END_TIMESTAMP) final long endTimestamp) {

        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
    }

    ///
    @Override
    public Bson toBsonFilter() {

        return Filters.and(Filters.gte(EntityField.TIMESTAMP, startTimestamp), Filters.lte(EntityField.TIMESTAMP, endTimestamp));
    }

    ///.
    protected String getTemporalField() {

        return EntityField.TIMESTAMP;
    }

    ///
}
