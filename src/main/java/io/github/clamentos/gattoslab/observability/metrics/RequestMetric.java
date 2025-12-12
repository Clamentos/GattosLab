package io.github.clamentos.gattoslab.observability.metrics;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///.
import org.bson.Document;
import org.bson.types.ObjectId;

///
@AllArgsConstructor
@Getter

///
public final class RequestMetric {

    ///
    private final long timestamp;
    private final String path;
    private final int latency;
    private final short httpStatus;

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
