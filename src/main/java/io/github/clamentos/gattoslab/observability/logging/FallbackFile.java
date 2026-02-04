package io.github.clamentos.gattoslab.observability.logging;

///
import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;

///.
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.persistence.MongoClientWrapper;
import io.github.clamentos.gattoslab.utils.BeanProvider;

///.
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

///.
import org.bson.Document;

///..
import org.jspecify.annotations.NonNull;

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
    public FallbackFile(@NonNull final AtomicReference<MongoClientWrapper> mongoClientReference, final long scheduleDelay, @NonNull final String filePath)
    throws IOException {

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

                    final MongoClientWrapper mongoDbClient = BeanProvider.getBean(MongoClientWrapper.class, "mongoClientWrapper");
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

            System.out.println("FallbackFile.run => " + exc);
        }

        this.dump();
    }

    ///..
    public void write(@NonNull final String log) throws IOException {

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

                final MongoCollection<Document> logsCollection = client.getCollection(DatabaseCollection.LOGS);
                lines.forEach(line -> logsCollection.insertOne(new LogEntity(line)));
            }

            // Clear the file.
            new FileWriter(filePath).close();
            session.commitTransaction();
        }

        catch(final Exception exc) {

            System.out.println("FallbackFile.dump => " + exc);
            if(exc instanceof MongoException) session.abortTransaction();
        }

        session.close();
        fileLock.unlock();
    }

    ///
}
