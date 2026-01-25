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

///.
import lombok.Getter;

///.
import org.bson.conversions.Bson;

///..
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

///
@Getter

///
public final class RequestMetricsSearchFilter extends TemporalSearchFilter {

    ///
    @Nullable private final Set<String> paths;
    @Nullable private final Set<Integer> httpStatuses;

    ///
    @JsonCreator
    public RequestMetricsSearchFilter(

        @JsonProperty("startTimestamp") final long startTimestamp,
        @JsonProperty("endTimestamp") final long endTimestamp,
        @JsonProperty("paths") @Nullable final Set<String> paths,
        @JsonProperty("httpStatuses") @Nullable final Set<Integer> httpStatuses
    ) {

        super(startTimestamp, endTimestamp);

        this.paths = paths;
        this.httpStatuses = httpStatuses;
    }

    ///
    @Override
    public @NonNull Bson toBsonFilter() {

        final List<Bson> filters = new ArrayList<>();

        filters.add(Filters.gte("timestamp", super.getStartTimestamp()));
        filters.add(Filters.lte("timestamp", super.getEndTimestamp()));

        if(paths != null && !paths.isEmpty()) filters.add(Filters.in("path", paths));
        if(httpStatuses != null && !httpStatuses.isEmpty()) filters.add(Filters.in("httpStatus", httpStatuses));

        return Filters.and(filters);
    }

    ///
}
