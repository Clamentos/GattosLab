package io.github.clamentos.gattoslab.observability.filters;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///..
import com.mongodb.client.model.Filters;

///.
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

///.
import lombok.Getter;

///.
import org.bson.conversions.Bson;

///..
import org.springframework.http.HttpStatus;

///
@Getter

///
public final class ChartSearchFilter extends TemporalSearchFilter {

    ///
    private final Set<String> chartTypes;
    private final Set<String> httpStatuses;

    ///
    @JsonCreator
    public ChartSearchFilter(

        @JsonProperty("startTimestamp") final long startTimestamp,
        @JsonProperty("endTimestamp") final long endTimestamp,
        @JsonProperty("chartTypes") final Set<String> chartTypes,
        @JsonProperty("httpStatuses") final Set<Integer> httpStatuses
    ) {

        super(startTimestamp, endTimestamp);

        this.chartTypes = chartTypes;
        this.httpStatuses = httpStatuses != null ? httpStatuses.stream().map(e -> HttpStatus.valueOf(e).toString()).collect(Collectors.toSet()) : null;
    }

    ///
    @Override
    public Bson toBsonFilter() {

        final List<Bson> filters = new ArrayList<>();

        filters.add(Filters.gte("timestamp", super.getStartTimestamp()));
        filters.add(Filters.lte("timestamp", super.getEndTimestamp()));

        if(chartTypes != null && !chartTypes.isEmpty()) filters.add(Filters.in("chartType", chartTypes));
        if(httpStatuses != null && !httpStatuses.isEmpty()) filters.add(Filters.in("httpStatus", httpStatuses));

        return Filters.and(filters);
    }

    ///
}
