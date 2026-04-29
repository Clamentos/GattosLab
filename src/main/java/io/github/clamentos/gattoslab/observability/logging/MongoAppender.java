package io.github.clamentos.gattoslab.observability.logging;

///
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.StackTraceElementProxy;
import ch.qos.logback.core.AppenderBase;

///..
import io.github.clamentos.gattoslab.observability.logging.entities.LogEntity;
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.persistence.MongoClientWrapper;
import io.github.clamentos.gattoslab.utils.ThreadSpawner;

///..
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

///..
import lombok.extern.slf4j.Slf4j;

///
@Slf4j(topic = "console_logger")

///
public final class MongoAppender extends AppenderBase<ILoggingEvent> {

    ///
    public static final String FALLBACK_FILE_PATH = "./fallback_logs.log";

    ///..
    private final AtomicReference<MongoClientWrapper> mongoClientReference;
    private final FallbackFile fallbackFile;

    ///..
    private final Thread dumper;

    ///
    public MongoAppender() throws IOException {

        super();

        mongoClientReference = new AtomicReference<>();
        fallbackFile = new FallbackFile(mongoClientReference, 1000L, FALLBACK_FILE_PATH);

        dumper = ThreadSpawner.spawnVirtualThread("gattos-lab-ff-dumper", fallbackFile);
    }

    ///
    @Override
    public void append(final ILoggingEvent logEvent) {

        if(logEvent != null) {

            try {

                final MongoClientWrapper client = mongoClientReference.get();

                if(client != null) client.getCollection(DatabaseCollection.LOGS).insertOne(new LogEntity(logEvent));
                else this.writeToFallbackFile(logEvent);
            }

            catch(final Exception exc) {

                log.error("Could not log", exc);
                this.writeToFallbackFile(logEvent);
            }
        }
    }

    ///..
    @Override
    public void stop() {

        try {

            fallbackFile.close();
            if(!dumper.join(Duration.ofSeconds(5))) log.warn("Timed-out while joining");

            final MongoClientWrapper client = mongoClientReference.get();
            if(client != null) client.getClient().close();
        }

        catch(final InterruptedException _) {

            log.error("Interrupted while joining, force quitting");
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

            sb.append(logEvent.getTimeStamp()).append(LogEntity.SECTION_SEPARATOR);
            sb.append(this.normalize(logEvent.getLevel())).append(LogEntity.SECTION_SEPARATOR);
            sb.append(this.normalize(logEvent.getThreadName())).append(LogEntity.SECTION_SEPARATOR);
            sb.append(this.normalize(logEvent.getLoggerName())).append(LogEntity.SECTION_SEPARATOR);
            sb.append(this.normalize(message).replace("\n", LogEntity.MESSAGE_LINE_SEPARATOR));

            if(throwableProxy != null) {

                final StackTraceElementProxy[] stacktrace = throwableProxy.getStackTraceElementProxyArray();

                sb.append(LogEntity.SECTION_SEPARATOR).append(throwableProxy.getClassName()).append(LogEntity.SECTION_SEPARATOR);
                sb.append(this.normalize(throwableProxy.getMessage()).replace("\n", LogEntity.MESSAGE_LINE_SEPARATOR));

                if(stacktrace != null) sb.append(LogEntity.SECTION_SEPARATOR).append(this.formatStacktraceForFile(stacktrace));
            }

            fallbackFile.write(sb.toString() + "\n");
        }

        catch(final IOException exc) {

            log.error("Could not log", exc);
        }
    }

    ///..
    private String normalize(final Object input) {

        if(input != null) {

            final String str = input.toString();
            return str != null ? str : LogEntity.NULL_REPLACEMENT;
        }

        return LogEntity.NULL_REPLACEMENT;
    }

    ///..
    private String formatStacktraceForFile(final StackTraceElementProxy[] stacktrace) {

        final StringBuilder traceString = new StringBuilder();

        for(final StackTraceElementProxy element : stacktrace) {

            traceString
                .append(this.normalize(element).replace("\n", LogEntity.MESSAGE_LINE_SEPARATOR))
                .append(LogEntity.SECTION_SEPARATOR)
            ;
        }

        if(!traceString.isEmpty()) traceString.deleteCharAt(traceString.length() - 1);
        return this.normalize(traceString.toString());
    }

    ///
}
