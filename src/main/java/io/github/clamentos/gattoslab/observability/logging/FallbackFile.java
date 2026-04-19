package io.github.clamentos.gattoslab.observability.logging;

///
import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;

///..
import io.github.clamentos.gattoslab.observability.logging.entities.LogEntity;
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.persistence.MongoClientProvider;
import io.github.clamentos.gattoslab.persistence.MongoClientWrapper;

///..
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

///..
import lombok.extern.slf4j.Slf4j;

///
@Slf4j(topic = "console_logger")

///
public final class FallbackFile implements Runnable {

    ///
    private final long scheduleDelay;
    private final String filePath;

    ///..
    private final AtomicReference<MongoClientWrapper> mongoClientReference;
    private final Path fallbackFilePath;
    private final Lock fileLock;

    ///
    public FallbackFile(final AtomicReference<MongoClientWrapper> mongoClientReference, final long scheduleDelay, final String filePath) throws IOException {

        this.scheduleDelay = scheduleDelay;
        this.filePath = filePath;

        this.mongoClientReference = mongoClientReference;

        fallbackFilePath = Path.of(filePath);
        fileLock = new ReentrantLock();

        if(!Files.exists(fallbackFilePath)) Files.createFile(fallbackFilePath);
    }

    ///
    @Override
    public void run() {

        while(true) {

            try {

                if(mongoClientReference.get() == null) {

                    final MongoClientWrapper mongoDbClient = MongoClientProvider.getWrapper();
                    if(mongoDbClient != null) mongoClientReference.set(mongoDbClient);
                }

                else {

                    this.dump();
                }

                Thread.sleep(scheduleDelay);
            }

            catch(final InterruptedException _) {

                this.dump();
                Thread.currentThread().interrupt();

                break;
            }

            catch(final Exception exc) {

                log.error("Could not log", exc);
                this.dump();
            }
        }
    }

    ///..
    public void write(final String log) throws IOException {

        fileLock.lock();
        Files.write(fallbackFilePath, log.getBytes(), StandardOpenOption.APPEND);
        fileLock.unlock();
    }

    ///.
    private void dump() {

        fileLock.lock();

        final MongoClientWrapper client = mongoClientReference.get();
        if(client == null) return;

        final ClientSession session = mongoClientReference.get().getClient().startSession();

        try {

            session.startTransaction();

            try(final Stream<String> lines = Files.lines(Path.of(filePath))) {

                final Date now = new Date();
                final MongoCollection<LogEntity> logsCollection = client.getCollection(DatabaseCollection.LOGS);

                lines.filter(Objects::nonNull).forEach(line -> logsCollection.insertOne(new LogEntity(line, now)));
            }

            // Clear the file.
            new FileWriter(filePath).close();
            session.commitTransaction();
        }

        catch(final Exception exc) {

            log.error("Could not log", exc);
            if(exc instanceof MongoException) session.abortTransaction();
        }

        session.close();
        fileLock.unlock();
    }

    ///
}
