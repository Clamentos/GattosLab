package io.github.clamentos.gattoslab.observability.metrics.entries;

///
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

///.
import lombok.Getter;

///.
import org.bson.Document;

///..
import org.jspecify.annotations.NonNull;

///
@Getter

///
public final class PathInvocationsEntry extends TrackerEntry {

    ///
    @NonNull private final Set<Short> httpStatuses;

    ///
    public PathInvocationsEntry(@NonNull final String path) {

        super(path);
        httpStatuses = ConcurrentHashMap.newKeySet();
    }

    ///..
    public PathInvocationsEntry(@NonNull final Document document) {

        super(document);

        httpStatuses = document.getList("httpStatuses", Integer.class)
            .stream()
            .map(Integer::shortValue)
            .collect(Collectors.toCollection(ConcurrentHashMap::newKeySet))
        ;
    }

    ///
    public void update(final long timestamp, final short httpStatus) {

        super.update(timestamp);
        httpStatuses.add(httpStatus);
    }

    ///..
    @Override
    public void merge(@NonNull final TrackerEntry trackerEntry) {

        super.merge(trackerEntry);
        httpStatuses.addAll(((PathInvocationsEntry)trackerEntry).getHttpStatuses());
    }

    ///..
    @Override
    public @NonNull Document toDocument() {

        return super.toDocument().append("httpStatuses", httpStatuses.stream().toList());
    }

    ///
}
