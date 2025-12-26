package io.github.clamentos.gattoslab.observability.metrics;

///
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.utils.FastAtomicCounter;

///.
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

///.
import lombok.Getter;

///.
import org.bson.Document;
import org.bson.types.ObjectId;

///..
import org.springframework.context.ApplicationEventPublisher;

///
public final class ObservabilityContext {

    ///
    private final Siphon siphon;

    @Getter
    private final Map<String, AtomicInteger> pathInvocationsTracker;

    @Getter
    private final Map<String, AtomicInteger> userAgentTracker;

    ///..
    private final FastAtomicCounter visitorCounter;

    ///
    public ObservabilityContext(final ApplicationEventPublisher applicationEventPublisher, final int siphonCapacity) {

        siphon = new Siphon(applicationEventPublisher, siphonCapacity);
        pathInvocationsTracker = new ConcurrentHashMap<>();
        userAgentTracker = new ConcurrentHashMap<>();
        visitorCounter = new FastAtomicCounter();
    }

    ///
    public boolean updateMetrics(final int processingTime, final int httpStatus, final String path, final String rawPath, final String userAgent) {

        visitorCounter.increment();
        final MetricsEntry metricsEntry = siphon.getNext();

        if(metricsEntry != null) {

            metricsEntry.setTimestamp(System.currentTimeMillis());
            metricsEntry.setPath(path);
            metricsEntry.setLatency(processingTime);
            metricsEntry.setHttpStatus((short)httpStatus);

            pathInvocationsTracker.computeIfAbsent(rawPath, _ -> new AtomicInteger()).incrementAndGet();
            userAgentTracker.computeIfAbsent(userAgent, _ -> new AtomicInteger()).incrementAndGet();

            visitorCounter.decrement();
            return true;
        }

        visitorCounter.decrement();
        return false;
    }

    ///.
    public Map<DatabaseCollection, List<Document>> toDocuments() {

        final long now = System.currentTimeMillis();
        final Map<DatabaseCollection, List<Document>> documents = new EnumMap<>(DatabaseCollection.class);

        documents.put(DatabaseCollection.REQUEST_METRICS, siphon.drain());
        documents.put(DatabaseCollection.PATHS_INVOCATIONS, this.toDocuments(pathInvocationsTracker, now));
        documents.put(DatabaseCollection.USER_AGENTS, this.toDocuments(userAgentTracker, now));

        return documents;
    }

    ///..
    public boolean isNoOneThere() {

        return visitorCounter.get() == 0;
    }

    ///..
    public void reset() {

        siphon.reset();
        pathInvocationsTracker.clear();
        userAgentTracker.clear();
    }

    ///.
    private List<Document> toDocuments(final Map<String, AtomicInteger> tracker, final long timestamp) {

        if(tracker.isEmpty()) return List.of();

        final List<Map<String, Object>> elements = new ArrayList<>();
        final Document document = new Document();

        document.append("_id", new ObjectId());
        document.append("timestamp", timestamp);
        document.append("elements", elements);

        for(final Map.Entry<String, AtomicInteger> count : tracker.entrySet()) {

            final Document innerDocument = new Document();
            innerDocument.append("name", count.getKey());
            innerDocument.append("count", count.getValue().get());

            elements.add(innerDocument);
        }

        return List.of(document);
    }

    ///
}
