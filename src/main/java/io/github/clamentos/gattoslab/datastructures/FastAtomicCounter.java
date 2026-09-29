package io.github.clamentos.gattoslab.datastructures;

///
import java.util.concurrent.atomic.AtomicLongArray;

///
public final class FastAtomicCounter {

    ///
    private final int dwordsPerCacheLine;
    private final AtomicLongArray paddedCounters;

    ///
    public FastAtomicCounter() {

        final int cacheLineSize = 64;
        final int elements = Runtime.getRuntime().availableProcessors() * cacheLineSize;

        this.dwordsPerCacheLine = cacheLineSize / 8;
        this.paddedCounters = new AtomicLongArray(elements);
    }

    ///
    public void increment() {

        this.paddedCounters.incrementAndGet(this.getIndex());
    }

    ///..
    public void reset() {

        for(int i = 0; i < this.paddedCounters.length(); i += this.dwordsPerCacheLine) {

            this.paddedCounters.set(i, 0);
        }
    }

    ///..
    public long get() {

        long total = 0;

        for(int i = 0; i < this.paddedCounters.length(); i += this.dwordsPerCacheLine) {

            total += this.paddedCounters.get(i);
        }

        return total;
    }

    ///.
    private int getIndex() {

        return (int)((Thread.currentThread().threadId() * this.dwordsPerCacheLine) % this.paddedCounters.length());
    }

    ///
}
