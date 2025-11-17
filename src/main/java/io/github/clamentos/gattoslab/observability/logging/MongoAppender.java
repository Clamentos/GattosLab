package io.github.clamentos.gattoslab.observability.logging;

///
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.AppenderBase;

///.
import io.github.clamentos.gattoslab.persistence.MongoClientWrapper;
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;

///.
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

///.
import org.bson.Document;
import org.bson.types.ObjectId;

///
@SuppressWarnings("squid:S106")

///
public final class MongoAppender extends AppenderBase<ILoggingEvent> {

    ///
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
        fallbackFile = new FallbackFile(mongoClientReference, 2_500L, "./fallback_logs.log");

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

        try {

            final StringBuilder sb = new StringBuilder();

            sb.append(logEvent.getTimeStamp()).append("\u0001");
            sb.append(logEvent.getLevel().toString()).append("\u0001");
            sb.append(logEvent.getThreadName()).append("\u0001");
            sb.append(logEvent.getLoggerName()).append("\u0001");
            sb.append(logEvent.getFormattedMessage().replace("\n", "\u0002"));

            final IThrowableProxy throwableProxy = logEvent.getThrowableProxy(); 

            if(throwableProxy != null) {

                final String message = throwableProxy.getMessage();

                sb.append("\u0001").append(throwableProxy.getClassName());
                if(message != null) sb.append("\u0001").append(message.replace("\n", "\u0002"));
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

            exception.append("name", throwableProxy.getClassName());
            exception.append("message", throwableProxy.getMessage());
            // stacktrace?

            document.append("exception", exception);
        }

        return document;
    }

    ///
}
