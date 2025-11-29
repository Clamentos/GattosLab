package io.github.clamentos.gattoslab.observability.filters;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///.
import com.mongodb.client.model.Filters;

///.
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

///.
import lombok.Getter;

///.
import org.bson.conversions.Bson;

///
@Getter

///
public final class LogSearchFilter extends TemporalSearchFilter {

    ///
    final Set<String> severities;
    final String threadPattern;
    final String loggerPattern;
    final String messagePattern;

    ///
    @JsonCreator
    public LogSearchFilter(

        @JsonProperty("startTimestamp") final long startTimestamp,
        @JsonProperty("endTimestamp") final long endTimestamp,
        @JsonProperty("severities") final Set<String> severities,
        @JsonProperty("threadPattern") final String threadPattern,
        @JsonProperty("loggerPattern") final String loggerPattern,
        @JsonProperty("messagePattern") final String messagePattern
    ) {

        super(startTimestamp, endTimestamp);

        this.severities = severities;
        this.threadPattern = threadPattern;
        this.loggerPattern = loggerPattern;
        this.messagePattern = messagePattern;
    }

    ///
    @Override
    public Bson toBsonFilter() {

        final List<Bson> filters = new ArrayList<>();

        filters.add(Filters.gte("timestamp", super.getStartTimestamp()));
        filters.add(Filters.lte("timestamp", super.getEndTimestamp()));

        if(severities != null && !severities.isEmpty()) filters.add(Filters.in("severity", severities));
        if(threadPattern != null && !threadPattern.isEmpty()) filters.add(Filters.regex("thread", threadPattern));
        if(loggerPattern != null && !loggerPattern.isEmpty()) filters.add(Filters.in("logger", threadPattern));
        if(messagePattern != null && !messagePattern.isEmpty()) filters.add(Filters.in("message", messagePattern));

        return Filters.and(filters);
    }

    ///
}
