package io.github.clamentos.gattoslab.observability.metrics;

///
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;

///.
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

///.
import org.bson.Document;
import org.bson.types.ObjectId;

///
public final class MetricsContainer {

    ///
    private final Queue<RequestMetric> requestMetrics;
    private final Map<String, AtomicInteger> pathsInvocationCounts;
    private final Map<String, AtomicInteger> userAgentCounts;

    ///
    public MetricsContainer() {

        requestMetrics = new ConcurrentLinkedQueue<>();
        pathsInvocationCounts = new ConcurrentHashMap<>();
        userAgentCounts = new ConcurrentHashMap<>();
    }

    ///..
    public void updateRequests(final int latency, final short status, final String path) {

        requestMetrics.add(new RequestMetric(System.currentTimeMillis(), path, latency, status));
    }

    ///..
    public void updatePathInvocations(final String path) {

        pathsInvocationCounts.computeIfAbsent(path, _ -> new AtomicInteger()).incrementAndGet();
    }

    ///..
    public void updateUserAgentCounts(final String userAgent) {

        userAgentCounts.computeIfAbsent(userAgent, _ -> new AtomicInteger()).incrementAndGet();
    }

    ///..
    public Map<DatabaseCollection, List<Document>> toDocuments() {

        final long now = System.currentTimeMillis();
        final Map<DatabaseCollection, List<Document>> entities = new EnumMap<>(DatabaseCollection.class);

        entities.put(DatabaseCollection.REQUEST_METRICS, this.metricsToDocument());
        entities.put(DatabaseCollection.PATHS_INVOCATIONS, this.invocationsToDocument(pathsInvocationCounts, now));
        entities.put(DatabaseCollection.USER_AGENTS, this.invocationsToDocument(userAgentCounts, now));

        return entities;
    }

    ///..
    public void merge(final MetricsContainer oldMetricsContainer) {

        requestMetrics.addAll(oldMetricsContainer.requestMetrics);
        pathsInvocationCounts.putAll(oldMetricsContainer.pathsInvocationCounts);
        userAgentCounts.putAll(oldMetricsContainer.userAgentCounts);
    }

    ///.
    private List<Document> metricsToDocument() {

        final List<Document> documents = new ArrayList<>();
        for(final RequestMetric requestMetric : requestMetrics) documents.add(requestMetric.toDocument());

        return documents;
    }

    ///..
    private List<Document> invocationsToDocument(final Map<String, AtomicInteger> counts, final long timestamp) {

        if(counts.isEmpty()) return List.of();

        final List<Map<String, Object>> elements = new ArrayList<>();
        final Document document = new Document();

        document.append("_id", new ObjectId());
        document.append("timestamp", timestamp);
        document.append("elements", elements);

        for(final Map.Entry<String, AtomicInteger> count : counts.entrySet()) {

            final Document innerDocument = new Document();
            innerDocument.append("name", count.getKey());
            innerDocument.append("count", count.getValue().get());

            elements.add(innerDocument);
        }

        return List.of(document);
    }

    ///
}
