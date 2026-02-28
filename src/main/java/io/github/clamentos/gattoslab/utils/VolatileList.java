package io.github.clamentos.gattoslab.utils;

///
import java.util.AbstractList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

///
public final class VolatileList<T> extends AbstractList<T> {

    ///
    private final AtomicReferenceArray<T> elements;
    private final AtomicInteger index;

    ///
    public VolatileList(final T[] elements) {

        this.elements = new AtomicReferenceArray<>(elements);
        index = new AtomicInteger();
    }

    ///
    @Override
    public int size() {

        return index.get();
    }

    ///..
    @Override
    public T get(final int index) {

        return elements.get(index);
    }

    ///..
    @Override
    public boolean equals(final Object other) {

        if(other == this) return true;
        if(!(other instanceof VolatileList)) return false;

        final VolatileList<?> otherCasted = (VolatileList<?>)other;
        if(otherCasted.size() != this.size()) return false;

        for(int i = 0; i < this.size(); i++) {

            final T thisElem = this.get(i);
            final Object otherElem = otherCasted.get(i);

            if(thisElem != null) {

                if(!thisElem.equals(otherElem)) return false;
            }

            else if(otherElem != null) return false;
        }

        return true;
    }

    ///..
    @Override
    public int hashCode() {

        int result = 1;
        for(int i = 0; i < elements.length(); i++) result = (result * 31) + Objects.hashCode(elements.get(i));

        return result;
    }

    ///
}
