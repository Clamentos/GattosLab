package io.github.clamentos.gattoslab.observability.metrics;

///
import io.github.clamentos.gattoslab.observability.metrics.entries.MetricsEntry;

///.
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

///.
import org.springframework.context.ApplicationEventPublisher;

///..
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

///
public final class Siphon {

    ///
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AtomicBoolean isDraining;

    ///..
    private final AtomicReferenceArray<MetricsEntry> elements;
    private final AtomicInteger index;

    ///
    public Siphon(@NonNull final ApplicationEventPublisher applicationEventPublisher, final int capacity) {

        this.applicationEventPublisher = applicationEventPublisher;
        isDraining = new AtomicBoolean();

        final MetricsEntry[] metricsEntries = new MetricsEntry[capacity];
        for(int i = 0; i < capacity; i++) metricsEntries[i] = new MetricsEntry();

        this.elements = new AtomicReferenceArray<>(metricsEntries);
        index = new AtomicInteger();
    }

    ///
    public @Nullable MetricsEntry getNext() {

        final int indexValue = index.getAndUpdate(val -> val < elements.length() ? val + 1 : val);

        if(indexValue < elements.length()) return elements.get(indexValue);
        if(isDraining.compareAndSet(false, true)) applicationEventPublisher.publishEvent(new DrainMetricsEvent());

        return null;
    }

    ///..
    public @NonNull List<MetricsEntry> drain() {

        isDraining.set(true);

        final int length = elements.length();
        final List<MetricsEntry> elementList = new ArrayList<>(length);

        for(int i = 0; i < index.get(); i++) {

            final MetricsEntry entity = elements.get(i);
            entity.createId();

            elementList.add(entity);
        }

        return elementList;
    }

    ///..
    public void reset() {

        index.set(0);
        isDraining.set(false);
    }

    ///
}
