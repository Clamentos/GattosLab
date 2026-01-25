package io.github.clamentos.gattoslab.observability.metrics;

///
import io.github.clamentos.gattoslab.observability.metrics.entries.MetricsEntry;
import io.github.clamentos.gattoslab.observability.metrics.entries.PathInvocationsEntry;
import io.github.clamentos.gattoslab.observability.metrics.entries.TrackerEntry;
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.utils.FastAtomicCounter;

///.
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

///.
import lombok.Getter;

import org.bson.Document;

///.
import org.springframework.context.ApplicationEventPublisher;

///..
import org.jspecify.annotations.NonNull;

///
public final class ObservabilityContext {

    ///
    private final Siphon siphon;

    @Getter
    private final Map<String, PathInvocationsEntry> pathInvocationsTracker;

    @Getter
    private final Map<String, TrackerEntry> userAgentTracker;

    ///..
    private final FastAtomicCounter visitorCounter;

    ///
    public ObservabilityContext(@NonNull final ApplicationEventPublisher applicationEventPublisher, final int siphonCapacity) {

        siphon = new Siphon(applicationEventPublisher, siphonCapacity);
        pathInvocationsTracker = new ConcurrentHashMap<>();
        userAgentTracker = new ConcurrentHashMap<>();
        visitorCounter = new FastAtomicCounter();
    }

    ///
    public boolean updateMetrics(

        final long startTime,
        final long endTime,
        final int httpStatus,
        @NonNull final String path,
        @NonNull final String rawPath,
        @NonNull final String userAgent
    ) {

        final MetricsEntry metricsEntry = siphon.getNext();

        if(metricsEntry != null) {

            visitorCounter.increment();

            metricsEntry.setTimestamp(endTime);
            metricsEntry.setPath(path);
            metricsEntry.setLatency((int)endTime - (int)startTime);
            metricsEntry.setHttpStatus((short)httpStatus);

            pathInvocationsTracker.computeIfAbsent(rawPath, _ -> new PathInvocationsEntry(rawPath)).update(endTime, (short)httpStatus);
            userAgentTracker.computeIfAbsent(userAgent, _ -> new TrackerEntry(userAgent)).update(endTime);

            visitorCounter.decrement();
            return true;
        }

        return false;
    }

    ///.
    public @NonNull Map<DatabaseCollection, List<Document>> toDocuments() {

        final Map<DatabaseCollection, List<Document>> documents = new EnumMap<>(DatabaseCollection.class);

        documents.put(DatabaseCollection.REQUEST_METRICS, siphon.drain());
        documents.put(DatabaseCollection.PATH_INVOCATIONS, pathInvocationsTracker.values().stream().map(PathInvocationsEntry::toDocument).toList());
        documents.put(DatabaseCollection.USER_AGENTS, userAgentTracker.values().stream().map(TrackerEntry::toDocument).toList());

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

    ///
}
