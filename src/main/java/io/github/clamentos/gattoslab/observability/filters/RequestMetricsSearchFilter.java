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
    @Nullable private final Boolean onlyOthers;
    @Nullable private final String pathPattern;
    @Nullable private final Set<Integer> httpStatuses;
    @Nullable private final String userAgentPattern;
    @NonNull private final Set<String> fieldsToExclude;

    ///
    @JsonCreator
    public RequestMetricsSearchFilter(

        @JsonProperty("startTimestamp") final long startTimestamp,
        @JsonProperty("endTimestamp") final long endTimestamp,
        @JsonProperty("onlyOthers") @Nullable final Boolean onlyOthers,
        @JsonProperty("pathPattern") @Nullable final String pathPattern,
        @JsonProperty("httpStatuses") @Nullable final Set<Integer> httpStatuses,
        @JsonProperty("userAgentPattern") @Nullable final String userAgentPattern,
        @JsonProperty("fieldsToExclude") @Nullable final Set<String> fieldsToExclude
    ) {

        super(startTimestamp, endTimestamp);

        this.onlyOthers = onlyOthers;
        this.pathPattern = pathPattern;
        this.httpStatuses = httpStatuses;
        this.userAgentPattern = userAgentPattern;
        this.fieldsToExclude = fieldsToExclude != null ? fieldsToExclude : Set.of();
    }

    ///
    @Override
    public @NonNull Bson toBsonFilter() {

        final List<Bson> filters = new ArrayList<>();

        filters.add(Filters.gte("timestamp", super.getStartTimestamp()));
        filters.add(Filters.lte("timestamp", super.getEndTimestamp()));

        if(onlyOthers != null) filters.add(Filters.eq("isOthers", onlyOthers));
        if(pathPattern != null && !pathPattern.isEmpty()) filters.add(Filters.regex("path", pathPattern));
        if(httpStatuses != null && !httpStatuses.isEmpty()) filters.add(Filters.in("httpStatus", httpStatuses));
        if(userAgentPattern != null && !userAgentPattern.isEmpty()) filters.add(Filters.regex("userAgent", userAgentPattern));

        return Filters.and(filters);
    }

    ///
}
