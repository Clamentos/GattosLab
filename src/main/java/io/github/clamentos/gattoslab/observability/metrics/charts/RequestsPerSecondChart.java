package io.github.clamentos.gattoslab.observability.metrics.charts;

///
import io.github.clamentos.gattoslab.utils.GenericUtils;

///.
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

///.
import lombok.Getter;

///.
import org.bson.Document;

///
@Getter

///
public final class RequestsPerSecondChart implements Chart<Void> {

    ///
    private final Map<Long, Long> labels; // Simulates an atomic set.
    private final Map<String, Map<Long, AtomicInteger>> datasets; // path -> timestamp -> counts

    ///
    public RequestsPerSecondChart() {

        labels = new ConcurrentHashMap<>();
        datasets = new ConcurrentHashMap<>();
    }

    ///
    @Override
    public void update(final long timestamp, final String path, final Void data) {

        labels.putIfAbsent(timestamp, timestamp);

        datasets

            .computeIfAbsent(path, _ -> new ConcurrentHashMap<>())
            .computeIfAbsent(timestamp, _ -> new AtomicInteger())
            .incrementAndGet()
        ;
    }

    ///..
    @Override
    public void updateTime(final long timestamp) {

        labels.putIfAbsent(timestamp, timestamp);
    }

    ///..
    @Override
    public Document toDocument() {

        final Document document = new Document();
        final List<Long> sortedLabels = labels.keySet().stream().sorted().toList();
        final List<Map<String, Object>> datasetList = new ArrayList<>();

        document.append("labels", sortedLabels);
        document.append("datasets", datasetList);

        for(final Map.Entry<String, Map<Long, AtomicInteger>> dataset : datasets.entrySet()) {

            final List<Integer> pointValues = GenericUtils.initList(sortedLabels.size(), 0);
            final Map<Long, AtomicInteger> value = dataset.getValue();

            for(int i = 0; i < pointValues.size(); i++) {

                final long timestamp = sortedLabels.get(i);
                final AtomicInteger count = value.get(timestamp);

                pointValues.set(i, count != null ? count.get() : 0);
            }

            final Document inner = new Document();
            inner.append("label", dataset.getKey());
            inner.append("data", pointValues);

            datasetList.add(inner);
        }

        return document;
    }

    ///
}
