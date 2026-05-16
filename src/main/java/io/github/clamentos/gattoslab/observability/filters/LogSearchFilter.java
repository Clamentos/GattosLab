package io.github.clamentos.gattoslab.observability.filters;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///..
import java.util.Set;

///..
import lombok.Getter;

///
@Getter

///
public final class LogSearchFilter extends SearchFilter {

    ///
    private final Set<String> severities;
    private final String threadPattern;
    private final String loggerPattern;
    private final String messagePattern;
    private final String exceptionClassPattern;

    ///
    @JsonCreator
    public LogSearchFilter(

        @JsonProperty("startTimestamp") final long startTimestamp,
        @JsonProperty("endTimestamp") final long endTimestamp,
        @JsonProperty("severities") final Set<String> severities,
        @JsonProperty("threadPattern") final String threadPattern,
        @JsonProperty("loggerPattern") final String loggerPattern,
        @JsonProperty("messagePattern") final String messagePattern,
        @JsonProperty("exceptionClassPattern") final String exceptionClassPattern
    ) {

        super(startTimestamp, endTimestamp);

        this.severities = severities;
        this.threadPattern = threadPattern;
        this.loggerPattern = loggerPattern;
        this.messagePattern = messagePattern;
        this.exceptionClassPattern = exceptionClassPattern;
    }

    ///
}
