package io.github.clamentos.gattoslab.observability.metrics;

///
import io.github.clamentos.gattoslab.observability.metrics.charts.Chart;
import io.github.clamentos.gattoslab.observability.metrics.charts.LatencyChart;
import io.github.clamentos.gattoslab.observability.metrics.charts.RequestsPerSecondChart;
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.utils.Pair;

///.
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

///.
import org.bson.Document;
import org.bson.types.ObjectId;

///
public final class MetricsContainer {

    ///
    private final Map<Integer, RequestsPerSecondChart> rpsCharts;
    private final Map<Integer, LatencyChart> latencyCharts;
    private final Map<String, AtomicInteger> pathsInvocationCounts;
    private final Map<String, AtomicInteger> userAgentCounts;

    ///
    public MetricsContainer(final List<Integer> httpStatuses, final Set<String> paths, final List<Pair<Integer, Integer>> latencyBuckets) {

        rpsCharts = new ConcurrentHashMap<>();
        latencyCharts = new ConcurrentHashMap<>();

        for(final Integer httpStatus : httpStatuses) {

            rpsCharts.put(httpStatus, new RequestsPerSecondChart(paths));
            latencyCharts.put(httpStatus, new LatencyChart(paths, latencyBuckets));
        }

        pathsInvocationCounts = new ConcurrentHashMap<>();
        userAgentCounts = new ConcurrentHashMap<>();
    }

    ///..
    public void updateRequests(final int status, final String path, final String truePath, final long timestamp, final int latency) {

        rpsCharts.get(status).update(timestamp, path, null);
        latencyCharts.get(status).update(timestamp, path, latency);
        pathsInvocationCounts.computeIfAbsent(truePath, _ -> new AtomicInteger()).incrementAndGet();
    }

    ///..
    public void updateUserAgents(final String userAgent) {

        userAgentCounts.computeIfAbsent(userAgent, _ -> new AtomicInteger()).incrementAndGet();
    }

    ///..
    public void updateTime(final long timestamp) {

        this.updateTime(rpsCharts.values(), timestamp);
        this.updateTime(latencyCharts.values(), timestamp);
    }

    ///..
    public Map<DatabaseCollection, List<Document>> toDocuments() {

        final long now = System.currentTimeMillis();
        final Map<DatabaseCollection, List<Document>> entities = new EnumMap<>(DatabaseCollection.class);
        final List<Document> charts = this.chartsToDocument(rpsCharts, now);

        charts.addAll(this.chartsToDocument(latencyCharts, now));
        entities.put(DatabaseCollection.CHARTS, charts);

        entities.put(DatabaseCollection.PATHS_INVOCATIONS, this.countsToDocument(pathsInvocationCounts, now));
        entities.put(DatabaseCollection.USER_AGENTS, this.countsToDocument(userAgentCounts, now));

        return entities;
    }

    ///.
    private void updateTime(final Collection<? extends Chart<?>> charts, final long timestamp) {

        for(final Chart<?> chart : charts) chart.updateTime(timestamp);
    }

    ///.
    private <T extends Chart<?>> List<Document> chartsToDocument(final Map<Integer, T> charts, final long timestamp) {

        final List<Document> documents = new ArrayList<>(rpsCharts.size() + latencyCharts.size());

        for(final Map.Entry<Integer, T> entry : charts.entrySet()) {

            final T chart = entry.getValue();
            final Document document = new Document();

            document.append("_id", new ObjectId());
            document.append("timestamp", timestamp);
            document.append("chartType", chart.getClass().getSimpleName());
            document.append("httpStatus", entry.getKey());
            document.append("chart", chart.toDocument());

            documents.add(document);
        }

        return documents;
    }

    ///..
    private List<Document> countsToDocument(final Map<String, AtomicInteger> counts, final long timestamp) {

        final List<Map<String, Object>> elements = new ArrayList<>(counts.size());
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
