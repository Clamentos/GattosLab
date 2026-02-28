package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

///..
import org.bson.types.ObjectId;

///
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

///
public final class RequestMetricsEntity {

    ///
    private ObjectId id;
    private long timestamp;
    private int latency;
    private String path;
    private String userAgent;
    private boolean isOthers;
    private int httpStatus;

    ///
}
