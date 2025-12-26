package io.github.clamentos.gattoslab.observability.metrics;

///
import lombok.Getter;
import lombok.Setter;

///.
import org.bson.Document;
import org.bson.types.ObjectId;

///
@Getter
@Setter

///
public final class MetricsEntry {

    ///
    private long timestamp;
    private String path;
    private int latency;
    private short httpStatus;

    ///
    public Document toDocument() {

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
