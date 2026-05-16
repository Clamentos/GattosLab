package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import com.fasterxml.jackson.annotation.JsonProperty;

///..
import io.github.clamentos.gattoslab.observability.filters.RequestMetricsSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.SearchFilter;
import io.github.clamentos.gattoslab.persistence.SearchableEntity;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

///
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

///
public final class RequestMetricsEntity implements SearchableEntity {

    ///
    private long timestamp;
    private int latency;
    private String path;
    private String userAgent;

    @Setter(onMethod = @__({@JsonProperty("isOthers")}))
    private boolean isOthers;

    private int httpStatus;

    ///
    @Override
    public boolean respectsFilter(final SearchFilter searchFilter) {

        final long startTimestamp = searchFilter.getStartTimestamp();
        final long endTimestamp = searchFilter.getEndTimestamp();

        boolean extraConditions = true;

        if(searchFilter instanceof final RequestMetricsSearchFilter requestMetricsSearchFilter) {

            if(requestMetricsSearchFilter.getOnlyOthers() != null) extraConditions &= isOthers == requestMetricsSearchFilter.getOnlyOthers().booleanValue();
            if(requestMetricsSearchFilter.getPathPattern() != null) extraConditions &= path.contains(requestMetricsSearchFilter.getPathPattern());
            if(requestMetricsSearchFilter.getHttpStatuses() != null) extraConditions &= requestMetricsSearchFilter.getHttpStatuses().contains(httpStatus);
            if(requestMetricsSearchFilter.getUserAgentPattern() != null) extraConditions &= userAgent.contains(requestMetricsSearchFilter.getUserAgentPattern());
        }

        return (timestamp >= startTimestamp && timestamp <= endTimestamp) && extraConditions;
    }

    ///..
    @Override
    public String toString() {

        return "{\"timestamp\":" + timestamp + ",\"latency\":" + latency + ",\"path\":\"" + path + "\",\"userAgent\":\"" + userAgent + "\",\"isOthers\":" + isOthers + ",\"httpStatus\":" + httpStatus + "}";
    }

    ///
}
