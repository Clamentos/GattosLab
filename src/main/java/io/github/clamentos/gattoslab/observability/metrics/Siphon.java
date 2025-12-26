package io.github.clamentos.gattoslab.observability.metrics;

///
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

///.
import org.bson.Document;

///..
import org.springframework.context.ApplicationEventPublisher;

///
public final class Siphon {

    ///
    private final ApplicationEventPublisher applicationEventPublisher;
    private final AtomicBoolean isDraining;

    ///..
    private final AtomicReferenceArray<MetricsEntry> elements;
    private final AtomicInteger index;

    ///
    public Siphon(final ApplicationEventPublisher applicationEventPublisher, final int capacity) {

        this.applicationEventPublisher = applicationEventPublisher;
        isDraining = new AtomicBoolean();

        final MetricsEntry[] metricsEntries = new MetricsEntry[capacity];
        for(int i = 0; i < capacity; i++) metricsEntries[i] = new MetricsEntry();

        this.elements = new AtomicReferenceArray<>(metricsEntries);
        index = new AtomicInteger();
    }

    ///
    public MetricsEntry getNext() {

        final int indexValue = index.getAndUpdate(val -> val < elements.length() ? val + 1 : val);

        if(indexValue < elements.length()) return elements.get(indexValue);
        if(isDraining.compareAndSet(false, true)) applicationEventPublisher.publishEvent(new DrainMetricsEvent());

        return null;
    }

    ///..
    public List<Document> drain() {

        final int length = elements.length();
        final List<Document> elementList = new ArrayList<>(length);

        for(int i = 0; i < index.get(); i++) elementList.add(elements.get(i).toDocument());

        return elementList;
    }

    ///..
    public void reset() {

        index.set(0);
        isDraining.set(false);
    }

    ///
}
