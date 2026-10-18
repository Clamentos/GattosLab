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
        final int length = Runtime.getRuntime().availableProcessors() * cacheLineSize;

        this.dwordsPerCacheLine = cacheLineSize / 8;
        this.paddedCounters = new AtomicLongArray(length);
    }

    ///
    public void increment() {

        final int index = (int)((Thread.currentThread().threadId() * this.dwordsPerCacheLine) % this.paddedCounters.length());
        this.paddedCounters.incrementAndGet(index);
    }

    ///..
    public void reset() {

        final int length = this.paddedCounters.length();

        for(int i = 0; i < length; i += this.dwordsPerCacheLine) {

            this.paddedCounters.set(i, 0);
        }
    }

    ///..
    public long get() {

        final int length = this.paddedCounters.length();
        long total = 0;

        for(int i = 0; i < length; i += this.dwordsPerCacheLine) {

            total += this.paddedCounters.get(i);
        }

        return total;
    }

    ///
}
