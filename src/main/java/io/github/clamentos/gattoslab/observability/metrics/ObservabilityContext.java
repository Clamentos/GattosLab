package io.github.clamentos.gattoslab.observability.metrics;

///
import io.github.clamentos.gattoslab.observability.metrics.entities.RequestMetricsEntity;
import io.github.clamentos.gattoslab.scheduling.eventbus.EventBus;
import io.github.clamentos.gattoslab.utils.FastAtomicCounter;

///..
import java.util.List;

///
public final class ObservabilityContext {

    ///
    private final Siphon siphon;
    private final FastAtomicCounter visitorCounter;

    ///
    public ObservabilityContext(final EventBus eventBus, final int siphonCapacity) {

        siphon = new Siphon(eventBus, siphonCapacity);
        visitorCounter = new FastAtomicCounter();
    }

    ///
    public boolean updateMetrics(final String path, final String userAgent, final boolean isOthers, final long startTime, final int httpStatus) {

        final RequestMetricsEntity entity = siphon.getNext();
        final long endTime = System.currentTimeMillis();

        if(entity != null) {

            visitorCounter.increment();

            entity.setPath(path);
            entity.setUserAgent(userAgent);
            entity.setOthers(isOthers);
            entity.setTimestamp(endTime);
            entity.setLatency((int)endTime - (int)startTime);
            entity.setHttpStatus((short)httpStatus);

            visitorCounter.decrement();
            return true;
        }

        return false;
    }

    ///.
    public List<RequestMetricsEntity> drainSiphon() {

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
