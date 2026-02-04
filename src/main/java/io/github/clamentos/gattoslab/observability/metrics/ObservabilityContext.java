package io.github.clamentos.gattoslab.observability.metrics;

///
import io.github.clamentos.gattoslab.observability.metrics.entries.MetricsEntry;
import io.github.clamentos.gattoslab.utils.FastAtomicCounter;

///.
import java.util.List;

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
    public @NonNull List<MetricsEntry> drainSiphon() {

        return siphon.drain();
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
