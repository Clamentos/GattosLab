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
public final class RequestMetricsSearchFilter extends AggregatedSearchFilter {

    ///
    private final Boolean onlyOthers;
    private final String pathPattern;
    private final Set<Integer> httpStatuses;
    private final String userAgentPattern;

    ///
    @JsonCreator
    public RequestMetricsSearchFilter(

        @JsonProperty(EntityField.START_TIMESTAMP) final long startTimestamp,
        @JsonProperty(EntityField.END_TIMESTAMP) final long endTimestamp,
        @JsonProperty(EntityField.BUCKET_SIZE) final Long bucketSize,
        @JsonProperty(EntityField.ONLY_OTHERS) final Boolean onlyOthers,
        @JsonProperty(EntityField.PATH_PATTERN) final String pathPattern,
        @JsonProperty(EntityField.HTTP_STATUSES) final Set<Integer> httpStatuses,
        @JsonProperty(EntityField.USER_AGENT_PATTERN) final String userAgentPattern
    ) {

        super(startTimestamp, endTimestamp, bucketSize);

        this.onlyOthers = onlyOthers;
        this.pathPattern = pathPattern;
        this.httpStatuses = httpStatuses;
        this.userAgentPattern = userAgentPattern;
    }

    ///
    @Override
    public Bson toBsonFilter() {

        final List<Bson> filters = new ArrayList<>();
        final String temporalFieldname = super.getTemporalField();

        filters.add(Filters.gte(temporalFieldname, super.getStartTimestamp()));
        filters.add(Filters.lte(temporalFieldname, super.getEndTimestamp()));

        if(onlyOthers != null) filters.add(Filters.eq(EntityField.IS_OTHERS, onlyOthers));
        if(pathPattern != null && !pathPattern.isEmpty()) filters.add(Filters.regex(EntityField.PATH, pathPattern));
        if(httpStatuses != null && !httpStatuses.isEmpty()) filters.add(Filters.in(EntityField.HTTP_STATUS, httpStatuses));
        if(userAgentPattern != null && !userAgentPattern.isEmpty()) filters.add(Filters.regex(EntityField.USER_AGENT, userAgentPattern));

        return Filters.and(filters);
    }

    ///
}
