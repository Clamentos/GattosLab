package io.github.clamentos.gattoslab.observability.metrics.charts;

///
import org.bson.Document;

///
public interface Chart<T> {

    ///
    void update(final long timestamp, final String path, final T data);
    void updateTime(final long timestamp);
    Document toDocument();

    ///
}
