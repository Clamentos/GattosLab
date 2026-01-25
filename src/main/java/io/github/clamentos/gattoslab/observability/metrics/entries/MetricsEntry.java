package io.github.clamentos.gattoslab.observability.metrics.entries;

///
import lombok.Getter;
import lombok.Setter;

///.
import org.bson.Document;
import org.bson.types.ObjectId;

///..
import org.jspecify.annotations.NonNull;

///
@Getter
@Setter

///
public final class MetricsEntry {

    ///
    private long timestamp;
    @NonNull private String path;
    private int latency;
    private short httpStatus;

    ///
    public @NonNull Document toDocument() {

        final Document document = new Document();

        document.append("_id", new ObjectId());
        document.append("timestamp", timestamp);
        document.append("path", path);
        document.append("latency", latency);
        document.append("httpStatus", httpStatus);

        return document;
    }

    ///
}
