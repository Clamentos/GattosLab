package io.github.clamentos.gattoslab.observability.logging;

///
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;

///.
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.persistence.MongoClientWrapper;

///.
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

///.
import org.bson.Document;
import org.bson.types.ObjectId;

///
@SuppressWarnings("squid:S106")

///
public final class MongoAppender extends AppenderBase<ILoggingEvent> {

    ///
    public static final String FALLBACK_FILE_PATH = "./fallback_logs.log";
    private static final long JOIN_TIMEOUT = 10_000L;

    ///..
    private final AtomicReference<MongoClientWrapper> mongoClientReference;
    private final FallbackFile fallbackFile;

    ///..
    private final Thread dumper;

    ///
    public MongoAppender() throws IOException {

        super();

        mongoClientReference = new AtomicReference<>();
        fallbackFile = new FallbackFile(mongoClientReference, 2_500L, FALLBACK_FILE_PATH);

        dumper = Thread.startVirtualThread(fallbackFile);
    }

    ///
    @Override
    public void append(final ILoggingEvent logEvent) {

        try {

            final MongoClientWrapper client = mongoClientReference.get();

            if(client != null) client.getCollection(DatabaseCollection.LOGS).insertOne(this.createLogDocument(logEvent));
            else this.writeToFallbackFile(logEvent);
        }

        catch(final Exception exc) {

            System.out.println("MongoAppender.append => " + exc);
            this.writeToFallbackFile(logEvent);
        }
    }

    ///..
    @Override
    public void stop() {

        try {

            dumper.interrupt();
            dumper.join(JOIN_TIMEOUT);

            final MongoClientWrapper client = mongoClientReference.get();
            if(client != null) client.getClient().close();
        }

        catch(final InterruptedException _) {

            System.out.println("MongoAppender.stop => interrupted while joining, force quitting");
            Thread.currentThread().interrupt();
        }

        super.stop();
    }

    ///.
    private void writeToFallbackFile(final ILoggingEvent logEvent) {

        /*
            1) timestamp|severity|thread|logger|message
            2) timestamp|severity|thread|logger|message|exceptionClass|exceptionMessage
            3) timestamp|severity|thread|logger|message|exceptionClass|exceptionMessage|trace1|trace2|trace3|...

            \u0001 to separate message sections & stacktrace entries, \u0002 as a placeholder for \n.
            \u0000 as a placeholder for nulls in special cases.
        */

        try {

            final StringBuilder sb = new StringBuilder();
            final String message = logEvent.getFormattedMessage();
            final IThrowableProxy throwableProxy = logEvent.getThrowableProxy(); 

            sb.append(logEvent.getTimeStamp()).append("\u0001");
            sb.append(this.normalize(logEvent.getLevel())).append("\u0001");
            sb.append(this.normalize(logEvent.getThreadName())).append("\u0001");
            sb.append(this.normalize(logEvent.getLoggerName())).append("\u0001");
            sb.append(this.normalize(message).replace("\n", "\u0002"));

            if(throwableProxy != null) {

                final StackTraceElementProxy[] stacktrace = throwableProxy.getStackTraceElementProxyArray();

                sb.append("\u0001").append(throwableProxy.getClassName()).append("\u0001");
                sb.append(this.normalize(throwableProxy.getMessage()).replace("\n", "\u0002"));

                if(stacktrace != null) sb.append("\u0001").append(this.formatStacktraceForFile(stacktrace));
            }

            sb.append("\n");
            fallbackFile.write(sb.toString());
        }

        catch(final IOException exc) {

            System.out.println("MongoAppender.writeToFallbackFile => " + exc);
        }
    }

    ///..
    private Document createLogDocument(final ILoggingEvent logEvent) {

        final Document document = new Document();

        document.append("_id", new ObjectId());
        document.append("timestamp", logEvent.getTimeStamp());
        document.append("severity", logEvent.getLevel().toString());
        document.append("thread", logEvent.getThreadName());
        document.append("logger", logEvent.getLoggerName());
        document.append("message", logEvent.getFormattedMessage());

        final IThrowableProxy throwableProxy = logEvent.getThrowableProxy();

        if(throwableProxy != null) {

            final Document exception = new Document();

            exception.append("className", throwableProxy.getClassName());
            exception.append("message", throwableProxy.getMessage());
            exception.append("stacktrace", this.formatStacktraceForDb(throwableProxy.getStackTraceElementProxyArray()));

            document.append("exception", exception);
        }

        return document;
    }

    ///..
    private String normalize(final Object input) {

        if(input == null) return "\u0000";
        else return input.toString();
    }

    ///..
    private List<String> formatStacktraceForDb(final StackTraceElementProxy[] stacktrace) {

        final List<String> formattedStacktrace = new ArrayList<>(stacktrace.length);

        for(int i = 0; i < stacktrace.length; i++) {

            final StackTraceElementProxy proxy = stacktrace[i];
            formattedStacktrace.add(proxy != null ? proxy.toString() : null);
        }

        return formattedStacktrace;
    }

    ///..
    private String formatStacktraceForFile(final StackTraceElementProxy[] stacktrace) {

        final StringBuilder sb = new StringBuilder();

        for(final StackTraceElementProxy element : stacktrace) {

            sb.append(this.normalize(element).replace("\n", "\u0002")).append("\u0001");
        }

        if(!sb.isEmpty()) sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    ///
}
