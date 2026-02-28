package io.github.clamentos.gattoslab.observability.metrics;

///
import io.github.clamentos.gattoslab.observability.metrics.entities.RequestMetricsEntity;
import io.github.clamentos.gattoslab.scheduling.eventbus.EventBus;

///..
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

///..
import org.bson.types.ObjectId;

///
public final class Siphon {

    ///
    private final EventBus eventBus;
    private final AtomicBoolean isDraining;

    ///..
    private final AtomicReferenceArray<RequestMetricsEntity> elements;
    private final AtomicInteger index;

    ///
    public Siphon(final EventBus eventBus, final int capacity) {

        this.eventBus = eventBus;
        isDraining = new AtomicBoolean();

        final RequestMetricsEntity[] entities = new RequestMetricsEntity[capacity];
        for(int i = 0; i < capacity; i++) entities[i] = new RequestMetricsEntity();

        this.elements = new AtomicReferenceArray<>(entities);
        index = new AtomicInteger();
    }

    ///
    public RequestMetricsEntity getNext() {

        final int indexValue = index.getAndUpdate(val -> val < elements.length() ? val + 1 : val);

        if(indexValue < elements.length()) return elements.get(indexValue);
        if(isDraining.compareAndSet(false, true)) eventBus.trigger();

        return null;
    }

    ///..
    public List<RequestMetricsEntity> drain() {

        isDraining.set(true);

        final int length = elements.length();
        final List<RequestMetricsEntity> elementList = new ArrayList<>(length);

        for(int i = 0; i < index.get(); i++) {

            final RequestMetricsEntity entity = elements.get(i);
            entity.setId(new ObjectId());

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
