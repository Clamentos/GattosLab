package io.github.clamentos.gattoslab.datastructures;

///
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;
import java.util.function.Supplier;

///
public final class Siphon<T extends Resettable> extends AbstractList<T> {

    ///
    private final Consumer<List<T>> drainTask;

    ///..
    private final AtomicReferenceArray<T> elements;
    private final AtomicInteger index;
    private final AtomicBoolean isDraining;
    private final FastAtomicCounter visitorCounter;

    ///
    public Siphon(final Class<T> type, final int capacity, final Supplier<T> generator, final Consumer<List<T>> drainTask) {

        this.drainTask = drainTask;

        @SuppressWarnings("unchecked")
        final T[] entities = (T[])Array.newInstance(type, capacity);

        for(int i = 0; i < capacity; i++) {

            entities[i] = generator.get();
        }

        this.elements = new AtomicReferenceArray<>(entities);
        this.index = new AtomicInteger();
        this.isDraining = new AtomicBoolean();
        this.visitorCounter = new FastAtomicCounter();
    }

    ///
    public boolean update(final Consumer<T> updateAction) {

        if(this.isDraining.get()) return false;
        final T element = this.getNext();

        if(element != null) {

            updateAction.accept(element);
            this.visitorCounter.increment();

            return true;
        }

        return false;
    }

    ///..
    public void drain() {

        if(this.isDraining.compareAndSet(false, true)) {

            this.drainTask.accept(this);
            this.clear();
        }
    }

    ///..
    public boolean isUpdating() {

        return this.visitorCounter.get() != this.index.get();
    }

    ///..
    @Override
    public void clear() {

        for(int i = 0; i < this.elements.length(); i++) {

            this.elements.get(i).reset();
        }

        this.visitorCounter.reset();
        this.index.set(0);
        this.isDraining.set(false);
    }

    ///..
    @Override
    public int size() {

        return this.index.get();
    }

    ///..
    @Override
    public T get(final int index) {

        return this.elements.get(index);
    }

    ///..
    @Override
    public boolean equals(final Object other) {

        return false;
    }

    ///..
    @Override
    public int hashCode() {

        return -1;
    }

    ///.
    private T getNext() {

        final int indexValue = this.index.getAndUpdate(val -> val < this.elements.length() ? (val + 1) : val);
        if(indexValue < this.elements.length()) return this.get(indexValue);

        if(this.isDraining.compareAndSet(false, true)) {

            GenericUtils.spawnVirtualThread("gattos-lab-siphon-drain-task", () -> {

                this.drainTask.accept(this);
                this.clear();
            });
        }

        return null;
    }

    ///
}
