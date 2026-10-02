package io.github.clamentos.gattoslab.datastructures;

///
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

///..
import lombok.EqualsAndHashCode;

///
@EqualsAndHashCode(callSuper = true)

///
public final class SortedList<T> extends AbstractList<T> {

    ///
    private final List<T> elements;
    private final Comparator<T> comparator;

    ///..
    private T maximum;

    ///
    public SortedList(final Comparator<T> comparator) {

        this.elements = new ArrayList<>();
        this.comparator = comparator;

        this.maximum = null;
    }

    ///
    @Override
    public int size() {

        return this.elements.size();
    }

    ///..
    @Override
    public T get(final int index) {

        return this.elements.get(index);
    }

    ///..
    @Override
    public boolean add(final T element) {

        if(this.maximum == null || this.comparator.compare(element, this.maximum) >= 0) {

            this.elements.add(element);
            this.maximum = element;
        }

        else {

            for(int i = this.size() - 2; i >= 0; i--) {

                if(this.comparator.compare(element, this.get(i)) >= 0) {

                    this.elements.add(i + 1, element);
                    break;
                }
            }
        }

        return true;
    }

    ///
}
