package io.github.clamentos.gattoslab.observability.metrics;

///
import io.github.clamentos.gattoslab.observability.metrics.entries.MetricsEntry;
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.utils.FastAtomicCounter;

///.
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

///.
import org.bson.Document;

///.
import org.springframework.context.ApplicationEventPublisher;

///..
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

///
public final class ObservabilityContext {

    ///
    private final Siphon siphon;
    private final FastAtomicCounter visitorCounter;

    ///
    public ObservabilityContext(@NonNull final ApplicationEventPublisher applicationEventPublisher, final int siphonCapacity) {

        siphon = new Siphon(applicationEventPublisher, siphonCapacity);
        visitorCounter = new FastAtomicCounter();
    }

    ///
    public boolean updateMetrics(

        @NonNull final String path,
        @Nullable final String userAgent,
        final boolean isOthers,
        final long startTime,
        final long endTime,
        final int httpStatus
    ) {

        final MetricsEntry metricsEntry = siphon.getNext();

        if(metricsEntry != null) {

            visitorCounter.increment();

            metricsEntry.setPath(path);
            metricsEntry.setUserAgent(userAgent);
            metricsEntry.setOthers(isOthers);
            metricsEntry.setTimestamp(endTime);
            metricsEntry.setLatency((int)endTime - (int)startTime);
            metricsEntry.setHttpStatus((short)httpStatus);

            visitorCounter.decrement();
            return true;
        }

        return false;
    }

    ///.
    public @NonNull Map<DatabaseCollection, List<Document>> toDocuments() {

        final Map<DatabaseCollection, List<Document>> documents = new EnumMap<>(DatabaseCollection.class);
        documents.put(DatabaseCollection.REQUEST_METRICS, siphon.drain());

        return documents;
    }

    ///..
    public boolean isNoOneThere() {

        return visitorCounter.get() == 0;
    }

    ///..
    public void reset() {

        siphon.reset();
    }

    ///
}
