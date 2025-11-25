package io.github.clamentos.gattoslab.observability.metrics.charts;

///
import io.github.clamentos.gattoslab.observability.metrics.charts.model.LatencyBucket;
import io.github.clamentos.gattoslab.utils.Pair;

///.
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

///.
import org.bson.Document;

///
public final class LatencyChart implements Chart<Integer> {

    ///
    private final Map<Long, Long> xAxis; // Simulates an atomic set.
    private final Map<String, Map<Long, List<LatencyBucket>>> datasets; // path -> timestamp -> latency counts

    ///..
    private final List<Pair<Integer, Integer>> latencyBuckets;

    ///
    public LatencyChart(final Set<String> paths, final List<Pair<Integer, Integer>> latencyBuckets) {

        xAxis = new ConcurrentHashMap<>();
        datasets = new ConcurrentHashMap<>();

        for(final String path : paths) datasets.put(path, new ConcurrentHashMap<>());

        this.latencyBuckets = latencyBuckets;
    }

    ///
    @Override
    public void update(final long timestamp, final String path, final Integer data) {

        Map<Long, List<LatencyBucket>> dataset = datasets.get(path);

        if(dataset == null) dataset = datasets.get("<other>");
        final List<LatencyBucket> buckets = dataset.computeIfAbsent(timestamp, _ -> latencyBuckets.stream().map(LatencyBucket::new).toList());

        for(final LatencyBucket bucket : buckets) {

            if(bucket.isWithin(data)) bucket.incrementCount();
        }
    }

    ///..
    @Override
    public void updateTime(final long timestamp) {

        xAxis.putIfAbsent(timestamp, timestamp);
    }

    ///..
    @Override
    public Document toDocument() {

        final Document document = new Document();
        final List<Long> sortedLabels = xAxis.keySet().stream().sorted().toList();
        final List<Map<String, Object>> datasetList = new ArrayList<>();

        document.append("datasets", datasetList);

        for(final Map.Entry<String, Map<Long, List<LatencyBucket>>> dataset : datasets.entrySet()) {

            final List<Document> pointValues = new ArrayList<>(sortedLabels.size());
            final Map<Long, List<LatencyBucket>> value = dataset.getValue();

            for(int i = 0; i < sortedLabels.size(); i++) {

                final long timestamp = sortedLabels.get(i);
                final List<LatencyBucket> buckets = value.get(timestamp);

                if(buckets != null) {

                    for(int j = 0; j < buckets.size(); j++) {

                        final Document axisDocument = new Document();
                        axisDocument.append("x", timestamp);
                        axisDocument.append("y", (long)j);
                        axisDocument.append("r", buckets.get(j).getCount().get());

                        pointValues.add(axisDocument);
                    }
                }
            }

            final Document innerDocument = new Document();
            innerDocument.append("label", dataset.getKey());
            innerDocument.append("data", pointValues);

            datasetList.add(innerDocument);
        }

        return document;
    }

    ///
}
