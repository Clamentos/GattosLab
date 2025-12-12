package io.github.clamentos.gattoslab.observability.metrics;

///
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.utils.Pair;

///.
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.bson.Document;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

///
@Component
@Slf4j

///
public final class ObservabilityContext {

    ///
    private final AtomicReference<MetricsContainer> container;
    private final SystemMetrics systemMetrics;

    ///..
    private final AtomicLong visitorCounter;

    ///
    @Autowired
    public ObservabilityContext(final SystemMetrics systemMetrics) {

        container = new AtomicReference<>(new MetricsContainer());
        this.systemMetrics = systemMetrics;

        visitorCounter = new AtomicLong();
    }

    ///
    public void updateRequests(final int latency, final short status, final String path) {

        this.updateMetrics(_ -> this.waitForAvailable().updateRequests(latency, status, path));
    }

    ///..
    public void updatePathInvocations(final String path) {

        this.updateMetrics(_ -> this.waitForAvailable().updatePathInvocations(path));
    }

    ///..
    public void updateUserAgentCounts(final String userAgent) {

        this.updateMetrics(_ -> this.waitForAvailable().updateUserAgentCounts(userAgent));
    }

    ///..
    public Pair<MetricsContainer, Map<DatabaseCollection, List<Document>>> dumpToDb() {

        final long currentVisitors = visitorCounter.getAndSet(-1);

        while(visitorCounter.get() + currentVisitors != -1) {

            try {

                Thread.sleep(25);
            }

            catch(final InterruptedException _) {

                Thread.currentThread().interrupt();
                log.warn("Interrupted while sleeping, force quitting");
            }
        }

        final MetricsContainer oldContainer = container.getAndSet(new MetricsContainer());
        visitorCounter.set(0);

        final Map<DatabaseCollection, List<Document>> metricsToSave = oldContainer.toDocuments();
        metricsToSave.put(DatabaseCollection.SYSTEM_METRICS, List.of(systemMetrics.toDocument()));

        return new Pair<>(oldContainer, metricsToSave);
    }

    ///..
    public void merge(final MetricsContainer oldMetricsContainer) {

        container.get().merge(oldMetricsContainer);
    }

    ///.
    private void updateMetrics(Consumer<Void> action) {

        try { action.accept(null); }
        catch(final Exception exc) { log.error("Could not update metrics", exc); }

        visitorCounter.decrementAndGet();
    }

    ///..
    private MetricsContainer waitForAvailable() {

        visitorCounter.getAndUpdate(val -> {

            if(val >= 0) return val + 1;
            return val;
        });

        return container.get();
    }

    ///
}
