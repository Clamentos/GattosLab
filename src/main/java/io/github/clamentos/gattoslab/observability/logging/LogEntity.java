package io.github.clamentos.gattoslab.observability.logging;

///
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;

///.
import java.util.ArrayList;
import java.util.List;

///.
import org.bson.Document;
import org.bson.types.ObjectId;

///.
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

///
public final class LogEntity extends Document {

    ///
    public LogEntity(@NonNull final ILoggingEvent logEvent) {

        this.put("_id", new ObjectId());
        this.put("timestamp", logEvent.getTimeStamp());
        this.put("severity", logEvent.getLevel().toString());
        this.put("thread", logEvent.getThreadName());
        this.put("logger", logEvent.getLoggerName());
        this.put("message", logEvent.getFormattedMessage());

        final IThrowableProxy throwableProxy = logEvent.getThrowableProxy();

        if(throwableProxy != null) {

            final Document exception = new Document();

            exception.put("className", throwableProxy.getClassName());
            exception.put("message", throwableProxy.getMessage());
            exception.put("stacktrace", this.formatStacktraceForDb(throwableProxy.getStackTraceElementProxyArray()));

            this.append("exception", exception);
        }
    }

    ///..
    public LogEntity(@NonNull final String log) {

        final String[] splits = log.split("\u0001");

        this.put("_id", new ObjectId());
        this.put("timestamp", Long.parseLong(splits[0]));
        this.put("severity", this.undoNormalization(splits[1]));
        this.put("thread", this.undoNormalization(splits[2]));
        this.put("logger", this.undoNormalization(splits[3]));
        this.put("message", this.undoNormalization(splits[4].replace("\u0002", "\n")));

        if(splits.length > 5) {

            final Document exception = new Document();

            exception.put("className", splits[5]);
            exception.put("message", this.undoNormalization(splits[6].replace("\u0002", "\n")));

            if(splits.length > 7) {

                final String[] stacktrace = new String[splits.length - 7];
                for(int i = 7; i < splits.length; i++) stacktrace[i - 7] = this.undoNormalization(splits[i].replace("\u0002", "\n"));

                exception.append("stacktrace", List.of(stacktrace));
            }

            this.put("exception", exception);
        }
    }

    ///
    private @NonNull List<String> formatStacktraceForDb(@NonNull final StackTraceElementProxy[] stacktrace) {

        final List<String> formattedStacktrace = new ArrayList<>(stacktrace.length);

        for(int i = 0; i < stacktrace.length; i++) {

            final StackTraceElementProxy proxy = stacktrace[i];
            formattedStacktrace.add(proxy != null ? proxy.toString() : null);
        }

        return formattedStacktrace;
    }

    ///..
    private @Nullable String undoNormalization(@NonNull final String input) {

        if("\u0000".equals(input)) return null;
        else return input;
    }

    ///
}
