package io.github.clamentos.gattoslab.observability.logging;

///
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;

///..
import java.util.ArrayList;
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
    private final LogEntityStackTrace exception;

    ///
    public LogEntity(final ILoggingEvent logEvent) {

        id = new ObjectId();
        timestamp = logEvent.getTimeStamp();
        severity = logEvent.getLevel().toString();
        thread = logEvent.getThreadName();
        logger = logEvent.getLoggerName();
        message = logEvent.getFormattedMessage();

        final IThrowableProxy throwableProxy = logEvent.getThrowableProxy();

        if(throwableProxy != null) {

            exception = new LogEntityStackTrace(

                throwableProxy.getClassName(),
                throwableProxy.getMessage(),
                this.formatStacktraceForDb(throwableProxy.getStackTraceElementProxyArray())
            );
        }

        else {

            exception = null;
        }
    }

    ///..
    public LogEntity(final String log) {

        final String[] splits = log.split(SECTION_SEPARATOR);

        id = new ObjectId();
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

                excStacktrace = new ArrayList<>();
                for(int i = 7; i < splits.length; i++) excStacktrace.add(this.undoNormalization(splits[i].replace(MESSAGE_LINE_SEPARATOR, "\n")));
            }

            exception = new LogEntityStackTrace(excClassName, excMessage, excStacktrace);
        }

        else {

            exception = null;
        }
    }

    ///
    private List<String> formatStacktraceForDb(final StackTraceElementProxy[] stacktrace) {

        if(stacktrace == null) return List.of();
        final List<String> formattedStacktrace = new ArrayList<>(stacktrace.length);

        for(int i = 0; i < stacktrace.length; i++) {

            final StackTraceElementProxy proxy = stacktrace[i];
            formattedStacktrace.add(proxy != null ? proxy.toString() : null);
        }

        return formattedStacktrace;
    }

    ///..
    private String undoNormalization(final String input) {

        if(NULL_REPLACEMENT.equals(input)) return null;
        else return input;
    }

    ///
}
