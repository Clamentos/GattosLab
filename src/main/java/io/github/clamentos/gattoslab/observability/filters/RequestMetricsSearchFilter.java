package io.github.clamentos.gattoslab.observability.filters;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///..
import com.mongodb.client.model.Filters;

///..
import io.github.clamentos.gattoslab.persistence.EntityField;

///..
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

///..
import lombok.Getter;

///..
import org.bson.conversions.Bson;

///
@Getter

///
public final class RequestMetricsSearchFilter extends TemporalSearchFilter {

    ///
    private final long bucketSize;
    private final Boolean onlyOthers;
    private final String pathPattern;
    private final Set<Integer> httpStatuses;
    private final String userAgentPattern;

    ///
    @JsonCreator
    public RequestMetricsSearchFilter(

        @JsonProperty("startTimestamp") final long startTimestamp,
        @JsonProperty("endTimestamp") final long endTimestamp,
        @JsonProperty("bucketSize") final long bucketSize,
        @JsonProperty("onlyOthers") final Boolean onlyOthers,
        @JsonProperty("pathPattern") final String pathPattern,
        @JsonProperty("httpStatuses") final Set<Integer> httpStatuses,
        @JsonProperty("userAgentPattern") final String userAgentPattern
    ) {

        super(startTimestamp, endTimestamp);

        this.bucketSize = bucketSize;
        this.onlyOthers = onlyOthers;
        this.pathPattern = pathPattern;
        this.httpStatuses = httpStatuses;
        this.userAgentPattern = userAgentPattern;
    }

    ///
    @Override
    public Bson toBsonFilter() {

        final List<Bson> filters = new ArrayList<>();

        filters.add(Filters.gte(super.getTemporalField().getField(), super.getStartTimestamp()));
        filters.add(Filters.lte(super.getTemporalField().getField(), super.getEndTimestamp()));

        if(onlyOthers != null) filters.add(Filters.eq(EntityField.IS_OTHERS.getField(), onlyOthers));
        if(pathPattern != null && !pathPattern.isEmpty()) filters.add(Filters.regex(EntityField.PATH.getField(), pathPattern));
        if(httpStatuses != null && !httpStatuses.isEmpty()) filters.add(Filters.in(EntityField.HTTP_STATUS.getField(), httpStatuses));
        if(userAgentPattern != null && !userAgentPattern.isEmpty()) filters.add(Filters.regex(EntityField.USER_AGENT.getField(), userAgentPattern));

        return Filters.and(filters);
    }

    ///
}
