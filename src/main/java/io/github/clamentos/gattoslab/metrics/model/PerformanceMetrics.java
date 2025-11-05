package io.github.clamentos.gattoslab.metrics.model;

///
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

///.
import org.springframework.http.HttpStatus;

///
public final class PerformanceMetrics {

    ///
    private final Map<HttpStatus, PerformanceMetricsEntry> front;
    private final Map<HttpStatus, PerformanceMetricsEntry> back;

    ///.
    private final AtomicBoolean frontOrBack;

    ///
    public PerformanceMetrics() {

        front = new ConcurrentHashMap<>();
        back = new ConcurrentHashMap<>();

        frontOrBack = new AtomicBoolean(true);
    }

    ///
    public Map<HttpStatus, PerformanceMetricsEntry> getMetricsMap() {

        return frontOrBack.get() ? front : back;
    }

    ///..
    public Map<HttpStatus, PerformanceMetricsEntry> swap() {

        final boolean direction = frontOrBack.get();
        final Map<HttpStatus, PerformanceMetricsEntry> oldMap = direction ? front : back;

        frontOrBack.set(!frontOrBack.get());
        return oldMap;
    }

    ///
}
