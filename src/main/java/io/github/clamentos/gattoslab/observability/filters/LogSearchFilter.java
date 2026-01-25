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
public final class LogSearchFilter extends TemporalSearchFilter {

    ///
    @Nullable private final Set<String> severities;
    @Nullable private final String threadPattern;
    @Nullable private final String loggerPattern;
    @Nullable private final String messagePattern;
    @Nullable private final String exceptionClassPattern;

    ///
    @JsonCreator
    public LogSearchFilter(

        @JsonProperty("startTimestamp") final long startTimestamp,
        @JsonProperty("endTimestamp") final long endTimestamp,
        @JsonProperty("severities") @Nullable final Set<String> severities,
        @JsonProperty("threadPattern") @Nullable final String threadPattern,
        @JsonProperty("loggerPattern") @Nullable final String loggerPattern,
        @JsonProperty("messagePattern") @Nullable final String messagePattern,
        @JsonProperty("exceptionClassPattern") @Nullable final String exceptionClassPattern
    ) {

        super(startTimestamp, endTimestamp);

        this.severities = severities;
        this.threadPattern = threadPattern;
        this.loggerPattern = loggerPattern;
        this.messagePattern = messagePattern;
        this.exceptionClassPattern = exceptionClassPattern;
    }

    ///
    @Override
    public @NonNull Bson toBsonFilter() {

        final List<Bson> filters = new ArrayList<>();

        filters.add(Filters.gte("timestamp", super.getStartTimestamp()));
        filters.add(Filters.lte("timestamp", super.getEndTimestamp()));

        if(severities != null && !severities.isEmpty()) filters.add(Filters.in("severity", severities));
        if(threadPattern != null && !threadPattern.isEmpty()) filters.add(Filters.regex("thread", threadPattern));
        if(loggerPattern != null && !loggerPattern.isEmpty()) filters.add(Filters.regex("logger", loggerPattern));
        if(messagePattern != null && !messagePattern.isEmpty()) filters.add(Filters.regex("message", messagePattern));
        if(exceptionClassPattern != null && !exceptionClassPattern.isEmpty()) filters.add(Filters.regex("exception.className", exceptionClassPattern));

        return Filters.and(filters);
    }

    ///
}
