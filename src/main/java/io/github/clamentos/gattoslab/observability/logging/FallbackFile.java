package io.github.clamentos.gattoslab.observability.logging;

///
import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;

///..
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.persistence.MongoClientProvider;
import io.github.clamentos.gattoslab.persistence.MongoClientWrapper;

///..
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

///
@SuppressWarnings("squid:S106")

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

        try {

            while(!Thread.currentThread().isInterrupted()) {

                if(mongoClientReference.get() == null) {

                    final MongoClientWrapper mongoDbClient = MongoClientProvider.getWrapper();
                    if(mongoDbClient != null) mongoClientReference.set(mongoDbClient);
                }

                else {

                    this.dump();
                }

                Thread.sleep(scheduleDelay);
            }
        }

        catch(final InterruptedException _) {

            Thread.currentThread().interrupt();
            Thread.interrupted();
        }

        catch(final Exception exc) {

            System.out.println(LocalDate.now() + ": FallbackFile.run => " + exc);
        }

        this.dump();
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

                final MongoCollection<LogEntity> logsCollection = client.getCollection(DatabaseCollection.LOGS);
                lines.filter(Objects::nonNull).forEach(line -> logsCollection.insertOne(new LogEntity(line)));
            }

            // Clear the file.
            new FileWriter(filePath).close();
            session.commitTransaction();
        }

        catch(final Exception exc) {

            System.out.println(LocalDate.now() + ": FallbackFile.dump => " + exc);
            if(exc instanceof MongoException) session.abortTransaction();
        }

        session.close();
        fileLock.unlock();
    }

    ///
}
