package io.github.clamentos.gattoslab.observability.metrics;

///
import io.github.clamentos.gattoslab.observability.metrics.system.SystemStatus;
import io.github.clamentos.gattoslab.utils.Pair;
import io.github.clamentos.gattoslab.utils.PropertyProvider;

///.
import jakarta.el.PropertyNotFoundException;

///.
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

///
@Component
@Slf4j

///
public final class ObservabilityContext {

    ///
    private final long limitTimeSlotIncrement;
    private final List<Pair<Integer, Integer>> latencyBuckets;

    ///..
    private final AtomicReference<MetricsContainer> container;
    private final SystemMetrics systemMetrics;

    ///..
    private final AtomicLong currentTimeSlot;
    private final AtomicLong limitTimeSlot;
    private final AtomicLong visitorCounter;

    ///
    @Autowired
    public ObservabilityContext(final SystemMetrics systemMetrics, final PropertyProvider propertyProvider) throws PropertyNotFoundException {

        latencyBuckets = Arrays

            .asList(propertyProvider.getProperty("app.metrics.latencyBuckets", String.class).split(","))
            .stream()
            .map(element -> {

                final String[] pair = element.split("-");
                return new Pair<>(Integer.parseInt(pair[0]), Integer.parseInt(pair[1]));
            })
            .toList()
        ;

        limitTimeSlotIncrement =

            propertyProvider.getProperty("app.metrics.rateCapacity", Long.class) *
            propertyProvider.getProperty("app.metrics.dumpToDbRate", Long.class)
        ;

        container = new AtomicReference<>(new MetricsContainer(latencyBuckets));
        this.systemMetrics = systemMetrics;

        final long now = System.currentTimeMillis();

        currentTimeSlot = new AtomicLong(now);
        limitTimeSlot = new AtomicLong(now + limitTimeSlotIncrement);
        visitorCounter = new AtomicLong();
    }

    ///
    public void updateRequests(final HttpStatus status, final String path, final int latency) {

        this.updateMetrics(_ -> this.waitForAvailable().updateRequests(status, path, currentTimeSlot.get(), latency));
    }

    ///..
    public void updateUserAgents(final String userAgent) {

        this.updateMetrics(_ -> this.waitForAvailable().updateUserAgents(userAgent));
    }

    ///..
    public SystemStatus getJvmMetrics() {

        return systemMetrics.getJvmMetrics();
    }

    ///..
    public MetricsContainer advance() {

        final long now = System.currentTimeMillis();
        currentTimeSlot.set(now);

        if(currentTimeSlot.get() >= limitTimeSlot.get()) {

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

            final MetricsContainer oldContainer = container.getAndSet(new MetricsContainer(latencyBuckets));

            limitTimeSlot.set(now + limitTimeSlotIncrement);
            visitorCounter.set(0);

            return oldContainer;
        }

        container.get().updateTime(now);
        return null;
    }

    ///.
    private void updateMetrics(Consumer<Void> action) {

        try {

            action.accept(null);
            visitorCounter.decrementAndGet();
        }

        catch(final Exception exc) {

            visitorCounter.decrementAndGet();
            log.error("Could not update metrics", exc);
        }
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
