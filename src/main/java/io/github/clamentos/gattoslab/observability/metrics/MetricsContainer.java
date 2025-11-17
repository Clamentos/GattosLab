package io.github.clamentos.gattoslab.observability.metrics;

///
import io.github.clamentos.gattoslab.observability.metrics.charts.Chart;
import io.github.clamentos.gattoslab.observability.metrics.charts.LatencyChart;
import io.github.clamentos.gattoslab.observability.metrics.charts.RequestsPerSecondChart;
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.utils.Pair;

///.
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

///.
import org.bson.Document;
import org.bson.types.ObjectId;

///..
import org.springframework.http.HttpStatus;

///
public final class MetricsContainer {

    ///
    private final Map<HttpStatus, RequestsPerSecondChart> rpsCharts;
    private final Map<HttpStatus, LatencyChart> latencyCharts;
    private final Map<String, AtomicInteger> pathsInvocationCounts;
    private final Map<String, AtomicInteger> userAgentCounts;

    ///..
    private final List<Pair<Integer, Integer>> latencyBuckets;

    ///
    public MetricsContainer(final List<Pair<Integer, Integer>> latencyBuckets) {

        rpsCharts = new ConcurrentHashMap<>();
        latencyCharts = new ConcurrentHashMap<>();
        pathsInvocationCounts = new ConcurrentHashMap<>();
        userAgentCounts = new ConcurrentHashMap<>();

        this.latencyBuckets = latencyBuckets;
    }

    ///..
    public void updateRequests(final HttpStatus status, final String path, final long timestamp, final int latency) {

        rpsCharts.computeIfAbsent(status, _ -> new RequestsPerSecondChart()).update(timestamp, path, null);
        latencyCharts.computeIfAbsent(status, _ -> new LatencyChart(latencyBuckets)).update(timestamp, path, latency);
        pathsInvocationCounts.computeIfAbsent(path, _ -> new AtomicInteger()).incrementAndGet();
    }

    ///..
    public void updateUserAgents(final String userAgent) {

        userAgentCounts.computeIfAbsent(userAgent, _ -> new AtomicInteger()).incrementAndGet();
    }

    ///..
    public void updateTime(final long timestamp) {

        for(final RequestsPerSecondChart rpsChart : rpsCharts.values()) rpsChart.updateTime(timestamp);
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
    private <T extends Chart<?>> List<Document> chartsToDocument(final Map<HttpStatus, T> charts, final long timestamp) {

        final List<Document> documents = new ArrayList<>(rpsCharts.size() + latencyCharts.size());

        for(final Map.Entry<HttpStatus, T> entry : charts.entrySet()) {

            final T chart = entry.getValue();
            final Document document = new Document();

            document.append("_id", new ObjectId());
            document.append("timestamp", timestamp);
            document.append("chartType", chart.getClass().getSimpleName());
            document.append("httpStatus", entry.getKey().toString());
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
