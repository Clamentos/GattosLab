package io.github.clamentos.gattoslab.observability.metrics;

///
import io.github.clamentos.gattoslab.observability.metrics.system.SystemStatus;
import io.github.clamentos.gattoslab.utils.Pair;
import io.github.clamentos.gattoslab.utils.PropertyProvider;
import io.github.clamentos.gattoslab.web.StaticSite;

///.
import jakarta.el.PropertyNotFoundException;

///.
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

///
@Component
@Slf4j

///
public final class ObservabilityContext {

    ///
    private final long limitTimeSlotIncrement;
    private final List<Pair<Integer, Integer>> latencyBuckets;
    private final List<Integer> httpStatuses;
    private final Set<String> paths;

    ///..
    private final AtomicReference<MetricsContainer> container;
    private final SystemMetrics systemMetrics;

    ///..
    private final AtomicLong currentTimeSlot;
    private final AtomicLong limitTimeSlot;
    private final AtomicLong visitorCounter;

    ///
    @Autowired
    public ObservabilityContext(final StaticSite staticSite, final SystemMetrics systemMetrics, final PropertyProvider propertyProvider)
    throws PropertyNotFoundException {

        limitTimeSlotIncrement =

            propertyProvider.getProperty("app.metrics.rateCapacity", Long.class) *
            propertyProvider.getProperty("app.metrics.dumpToDbRate", Long.class)
        ;

        latencyBuckets = Arrays

            .asList(propertyProvider.getProperty("app.metrics.latencyBuckets", String.class).split(","))
            .stream()
            .map(element -> {

                final String[] pair = element.split("-");
                return new Pair<>(Integer.parseInt(pair[0]), Integer.parseInt(pair[1]));
            })
            .toList()
        ;

        httpStatuses = Arrays

            .asList(propertyProvider.getProperty("app.metrics.httpStatuses", String.class).split(","))
            .stream()
            .map(Integer::parseInt)
            .collect(Collectors.toCollection(ArrayList::new))
        ;

        httpStatuses.add(-1); // Used as a "catch-all" value.

        paths = new HashSet<>(staticSite.getPaths());
        paths.add("<other>");

        container = new AtomicReference<>(new MetricsContainer(httpStatuses, paths, latencyBuckets));
        this.systemMetrics = systemMetrics;

        final long now = System.currentTimeMillis();

        currentTimeSlot = new AtomicLong(now);
        limitTimeSlot = new AtomicLong(now + limitTimeSlotIncrement);
        visitorCounter = new AtomicLong();
    }

    ///
    public void updateRequests(final int status, final String path, final String truePath, final int latency) {

        final int actualStatus = httpStatuses.contains(status) ? status : -1;
        this.updateMetrics(_ -> this.waitForAvailable().updateRequests(actualStatus, path, truePath, currentTimeSlot.get(), latency));
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

        if(now >= limitTimeSlot.get()) {

            currentTimeSlot.set(now);
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

            final MetricsContainer oldContainer = container.getAndSet(new MetricsContainer(httpStatuses, paths, latencyBuckets));

            container.get().updateTime(now);
            limitTimeSlot.set(now + limitTimeSlotIncrement);
            visitorCounter.set(0);

            return oldContainer;
        }

        else {

            currentTimeSlot.set(now);
            container.get().updateTime(now);

            return null;
        }
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
