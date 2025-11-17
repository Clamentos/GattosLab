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
    final Set<String> threads;
    final Set<String> loggers;

    ///
    @JsonCreator
    public LogSearchFilter(

        @JsonProperty("startTimestamp") long startTimestamp,
        @JsonProperty("endTimestamp") long endTimestamp,
        @JsonProperty("severities") Set<String> severities,
        @JsonProperty("threads") Set<String> threads,
        @JsonProperty("loggers") Set<String> loggers
    ) {

        super(startTimestamp, endTimestamp);

        this.severities = severities;
        this.threads = threads;
        this.loggers = loggers;
    }

    ///
    @Override
    public Bson toBsonFilter() {

        final List<Bson> filters = new ArrayList<>();

        filters.add(Filters.gte("timestamp", super.getStartTimestamp()));
        filters.add(Filters.lte("timestamp", super.getEndTimestamp()));

        if(severities != null && !severities.isEmpty()) filters.add(Filters.in("severity", severities));
        if(threads != null && !threads.isEmpty()) filters.add(Filters.in("thread", threads));
        if(loggers != null && !loggers.isEmpty()) filters.add(Filters.in("logger", loggers));

        return Filters.and(filters);
    }

    ///
}
