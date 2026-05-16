package io.github.clamentos.gattoslab.observability.logging.entities;

///
import io.github.clamentos.gattoslab.observability.filters.LogSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.SearchFilter;
import io.github.clamentos.gattoslab.persistence.SearchableEntity;

///..
import java.util.ArrayList;
import java.util.List;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class LogEntity implements SearchableEntity {

    ///
    public static final String NULL_REPLACEMENT = "\u0000";
    public static final String SECTION_SEPARATOR = "\u0001";
    public static final String MESSAGE_LINE_SEPARATOR = "\u0002";

    ///
    private final long timestamp;
    private final String severity;
    private final String thread;
    private final String logger;
    private final String message;
    private final LogEntityExceptionEntry exception;

    ///
    public LogEntity(final String log) {

        final String[] splits = log.split(SECTION_SEPARATOR);

        timestamp = Long.parseLong(splits[0]);
        severity = this.undoNormalization(splits[1]);
        thread = this.undoNormalization(splits[2]);
        logger = this.undoNormalization(splits[3]);
        message = this.undoNormalization(splits[4].replace(MESSAGE_LINE_SEPARATOR, "\n"));

        if(splits.length > 5) {

            final String excClassName = splits[5];
            final String excMessage = this.undoNormalization(splits[6].replace(MESSAGE_LINE_SEPARATOR, "\n"));
            List<String> excStacktrace = null;

            if(splits.length > 7) {

                excStacktrace = new ArrayList<>(splits.length - 7);
                for(int i = 7; i < splits.length; i++) excStacktrace.add(this.undoNormalization(splits[i].replace(MESSAGE_LINE_SEPARATOR, "\n")));
            }

            exception = new LogEntityExceptionEntry(excClassName, excMessage, excStacktrace);
        }

        else {

            exception = null;
        }
    }

    ///
    @Override
    public boolean respectsFilter(final SearchFilter searchFilter) {

        final long startTimestamp = searchFilter.getStartTimestamp();
        final long endTimestamp = searchFilter.getEndTimestamp();

        boolean extraConditions = true;

        if(searchFilter instanceof final LogSearchFilter logSearchFilter) {

            if(logSearchFilter.getSeverities() != null) extraConditions &= logSearchFilter.getSeverities().contains(severity);
            if(logSearchFilter.getThreadPattern() != null) extraConditions &= thread.contains(logSearchFilter.getThreadPattern());
            if(logSearchFilter.getLoggerPattern() != null) extraConditions &= logger.contains(logSearchFilter.getLoggerPattern());
            if(logSearchFilter.getMessagePattern() != null) extraConditions &= message.contains(logSearchFilter.getMessagePattern());

            if(logSearchFilter.getExceptionClassPattern() != null && exception != null) {

                extraConditions &= exception.getClassName().contains(logSearchFilter.getExceptionClassPattern());
            }
        }

        return (timestamp >= startTimestamp && timestamp <= endTimestamp) && extraConditions;
    }

    ///
    private String undoNormalization(final String input) {

        if(NULL_REPLACEMENT.equals(input)) return null;
        else return input;
    }

    ///
}
