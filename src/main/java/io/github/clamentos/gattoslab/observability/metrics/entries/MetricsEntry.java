package io.github.clamentos.gattoslab.observability.metrics.entries;

///
import lombok.Getter;
import lombok.Setter;

///.
import org.bson.Document;
import org.bson.types.ObjectId;

///..
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

///
@Getter
@Setter

///
public final class MetricsEntry {

    ///
    @NonNull private String path;
    @Nullable private String userAgent;
    private boolean isOthers;
    private long timestamp;
    private int latency;
    private short httpStatus;

    ///
    public @NonNull Document toDocument() {

        final Document document = new Document();

        document.append("_id", new ObjectId());
        document.append("path", path);
        document.append("userAgent", userAgent);
        document.append("isOthers", isOthers);
        document.append("timestamp", timestamp);
        document.append("latency", latency);
        document.append("httpStatus", httpStatus);

        return document;
    }

    ///
}
