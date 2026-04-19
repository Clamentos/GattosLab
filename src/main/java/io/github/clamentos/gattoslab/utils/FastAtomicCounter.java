package io.github.clamentos.gattoslab.utils;

///
import java.util.concurrent.atomic.AtomicLongArray;

///
public final class FastAtomicCounter {

    ///
    private final AtomicLongArray paddedCounters;

    ///
    public FastAtomicCounter() {

        paddedCounters = new AtomicLongArray(512);
    }

    ///
    public void increment() {

        paddedCounters.incrementAndGet(this.getIndex());
    }

    ///..
    public void decrement() {

        paddedCounters.decrementAndGet(this.getIndex());
    }

    ///..
    public long get() {

        long total = 0;
        for(int i = 0; i < paddedCounters.length(); i += 8) total += paddedCounters.get(i);

        return total;
    }

    ///.
    private int getIndex() {

        return (int)((Thread.currentThread().threadId() << 3) & 511);
    }

    ///
}
