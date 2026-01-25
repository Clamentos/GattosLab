package io.github.clamentos.gattoslab.observability.metrics.entries;

///
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

///.
import lombok.Getter;

///.
import org.bson.Document;
import org.bson.types.ObjectId;

///..
import org.jspecify.annotations.NonNull;

///
@Getter

///
public class TrackerEntry {

    ///
    @NonNull private final String key;
    @NonNull private final AtomicInteger count;
    @NonNull private final AtomicLong timestamp;

    ///
    public TrackerEntry(@NonNull final String key) {

        this.key = key;

        timestamp = new AtomicLong();
        count = new AtomicInteger();
    }

    ///..
    public TrackerEntry(@NonNull final Document document) {

        key = document.getString("key");
        count = new AtomicInteger(document.getInteger("count"));
        timestamp = new AtomicLong(document.getLong("timestamp"));
    }

    ///
    public void update(final long timestamp) {

        count.incrementAndGet();
        this.timestamp.set(timestamp);
    }

    ///..
    public void merge(@NonNull final TrackerEntry trackerEntry) {

        count.addAndGet(trackerEntry.getCount().get());

        final long otherTimestamp = trackerEntry.getTimestamp().get();
        if(timestamp.get() < otherTimestamp) timestamp.set(otherTimestamp);
    }

    ///..
    public @NonNull Document toDocument() {

        final Document document = new Document();

        document.append("_id", new ObjectId());
        document.append("key", key);
        document.append("count", count.get());
        document.append("timestamp", timestamp.get());

        return document;
    }

    ///
}
