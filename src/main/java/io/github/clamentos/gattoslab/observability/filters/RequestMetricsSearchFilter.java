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

        @JsonProperty(EntityField.START_TIMESTAMP) final long startTimestamp,
        @JsonProperty(EntityField.END_TIMESTAMP) final long endTimestamp,
        @JsonProperty(EntityField.BUCKET_SIZE) final long bucketSize,
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

        filters.add(Filters.gte(super.getTemporalField(), super.getStartTimestamp()));
        filters.add(Filters.lte(super.getTemporalField(), super.getEndTimestamp()));

        if(onlyOthers != null) filters.add(Filters.eq(EntityField.IS_OTHERS, onlyOthers));
        if(pathPattern != null && !pathPattern.isEmpty()) filters.add(Filters.regex(EntityField.PATH, pathPattern));
        if(httpStatuses != null && !httpStatuses.isEmpty()) filters.add(Filters.in(EntityField.HTTP_STATUS, httpStatuses));
        if(userAgentPattern != null && !userAgentPattern.isEmpty()) filters.add(Filters.regex(EntityField.USER_AGENT, userAgentPattern));

        return Filters.and(filters);
    }

    ///
}
