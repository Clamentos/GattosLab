package io.github.clamentos.gattoslab.datastructures;

///
import java.util.concurrent.atomic.AtomicLongArray;

///
public final class FastAtomicCounter {

    ///
    private static final int CPU_CACHE_LINE_SIZE_BYTES = 64;

    ///.
    private final int dwordsPerCacheLine;
    private final AtomicLongArray paddedCounters;

    ///
    public FastAtomicCounter() {

        final int elements = Runtime.getRuntime().availableProcessors() * CPU_CACHE_LINE_SIZE_BYTES;

        this.dwordsPerCacheLine = CPU_CACHE_LINE_SIZE_BYTES / 8;
        this.paddedCounters = new AtomicLongArray(elements);
    }

    ///
    public void increment() {

        this.paddedCounters.incrementAndGet(this.getIndex());
    }

    ///..
    public void decrement() {

        this.paddedCounters.decrementAndGet(this.getIndex());
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

    ///.
    private int getIndex() {

        return (int)((Thread.currentThread().threadId() * this.dwordsPerCacheLine) % this.paddedCounters.length());
    }

    ///
}
