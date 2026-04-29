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
public final class LogSearchFilter extends TemporalSearchFilter {

    ///
    private final Set<String> severities;
    private final String threadPattern;
    private final String loggerPattern;
    private final String messagePattern;
    private final String exceptionClassPattern;

    ///
    @JsonCreator
    public LogSearchFilter(

        @JsonProperty(EntityField.START_TIMESTAMP) final long startTimestamp,
        @JsonProperty(EntityField.END_TIMESTAMP) final long endTimestamp,
        @JsonProperty(EntityField.SEVERITIES) final Set<String> severities,
        @JsonProperty(EntityField.THREAD_PATTERN) final String threadPattern,
        @JsonProperty(EntityField.LOGGER_PATTERN) final String loggerPattern,
        @JsonProperty(EntityField.MESSAGE_PATTERN) final String messagePattern,
        @JsonProperty(EntityField.EXCEPTION_CLASS_PATTERN) final String exceptionClassPattern
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
    public Bson toBsonFilter() {

        final List<Bson> filters = new ArrayList<>();
        final String temporalFieldname = super.getTemporalField();

        filters.add(Filters.gte(temporalFieldname, super.getStartTimestamp()));
        filters.add(Filters.lte(temporalFieldname, super.getEndTimestamp()));

        if(severities != null && !severities.isEmpty()) filters.add(Filters.in(EntityField.SEVERITY, severities));
        if(threadPattern != null && !threadPattern.isEmpty()) filters.add(Filters.regex(EntityField.THREAD, threadPattern));
        if(loggerPattern != null && !loggerPattern.isEmpty()) filters.add(Filters.regex(EntityField.LOGGER, loggerPattern));
        if(messagePattern != null && !messagePattern.isEmpty()) filters.add(Filters.regex(EntityField.MESSAGE, messagePattern));

        if(exceptionClassPattern != null && !exceptionClassPattern.isEmpty()) {

            filters.add(Filters.regex(EntityField.EXCEPTION + "." + EntityField.CLASS_NAME, exceptionClassPattern));
        }

        return Filters.and(filters);
    }

    ///
}
