package io.github.clamentos.gattoslab.metrics.model;

///
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

///
public final class PerformanceMetrics {

    ///
    private final Map<String, PerformanceEntry> front;
    private final Map<String, PerformanceEntry> back;

    ///.
    private final AtomicBoolean frontOrBack;

    ///
    public PerformanceMetrics() {

        front = new ConcurrentHashMap<>();
        back = new ConcurrentHashMap<>();

        frontOrBack = new AtomicBoolean(true);
    }

    ///
    public Map<String, PerformanceEntry> getMetricsMap() {

        return frontOrBack.get() ? front : back;
    }

    ///..
    public Map<String, PerformanceEntry> swap() {

        final boolean direction = frontOrBack.get();
        final Map<String, PerformanceEntry> oldMap = direction ? front : back;

        frontOrBack.set(!frontOrBack.get());
        return oldMap;
    }

    ///
}
