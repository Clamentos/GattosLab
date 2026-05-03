package io.github.clamentos.gattoslab.observability.logging.entities;

///
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;

///..
import io.github.clamentos.gattoslab.exceptions.CauseContainer;

///..
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///..
import org.bson.types.ObjectId;

///
@AllArgsConstructor
@Getter

///
public final class LogEntity {

    ///
    public static final String NULL_REPLACEMENT = "\u0000";
    public static final String SECTION_SEPARATOR = "\u0001";
    public static final String MESSAGE_LINE_SEPARATOR = "\u0002";

    ///
    private final ObjectId id;
    private final long timestamp;
    private final String severity;
    private final String thread;
    private final String logger;
    private final String message;
    private final LogEntityExceptionEntry exception;

    ///
    public LogEntity(final ILoggingEvent logEvent) {

        final long logbackTimestamp = logEvent.getTimeStamp();

        id = new ObjectId(new Date(logbackTimestamp));
        timestamp = logbackTimestamp;
        severity = logEvent.getLevel().toString();
        thread = logEvent.getThreadName();
        logger = logEvent.getLoggerName();
        message = logEvent.getFormattedMessage();

        final IThrowableProxy throwableProxy = logEvent.getThrowableProxy();

        if(throwableProxy != null) {

            final List<String> stacktrace = this.formatStacktraceForDb(throwableProxy.getStackTraceElementProxyArray());

            this.formatStacktraceForDb(throwableProxy, stacktrace);
            exception = new LogEntityExceptionEntry(throwableProxy.getClassName(), throwableProxy.getMessage(), stacktrace);
        }

        else {

            exception = null;
        }
    }

    ///..
    public LogEntity(final String log, final Date now) {

        final String[] splits = log.split(SECTION_SEPARATOR);

        id = new ObjectId(now);
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
    private List<String> formatStacktraceForDb(final StackTraceElementProxy[] stacktrace) {

        if(stacktrace == null) return new ArrayList<>();
        final List<String> formattedStacktrace = new ArrayList<>(stacktrace.length);

        for(int i = 0; i < stacktrace.length; i++) {

            final StackTraceElementProxy proxy = stacktrace[i];
            formattedStacktrace.add(proxy != null ? proxy.toString() : null);
        }

        return formattedStacktrace;
    }

    ///..
    private void formatStacktraceForDb(final IThrowableProxy exception, final List<String> stacktrace) {

        if(exception != null) {

            final String className = exception.getClassName();
            final String msg = exception.getMessage();
            final IThrowableProxy cause = exception.getCause();

            if(cause != null) {

                if(cause.getClassName().equals(CauseContainer.class.getName())) stacktrace.add("$" + className + ": (" + cause.getMessage() + ") ~ " + msg);
                else stacktrace.add("$" + className + ": " + msg);

                this.formatStacktraceForDb(cause.getCause(), stacktrace);
            }

            else {

                stacktrace.add("$" + className + ": " + msg);
            }
        }
    }

    ///..
    private String undoNormalization(final String input) {

        if(NULL_REPLACEMENT.equals(input)) return null;
        else return input;
    }

    ///
}
