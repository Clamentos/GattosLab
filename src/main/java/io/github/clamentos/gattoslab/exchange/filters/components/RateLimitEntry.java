package io.github.clamentos.gattoslab.exchange.filters.components;

///
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

///..
import lombok.Getter;

///
@Getter

///
public final class RateLimitEntry {

    ///
    private final AtomicLong requestCounter;
    private final AtomicInteger blockCounter;

    ///
    public RateLimitEntry(final int maxRequestsPerIp, final int blockCounterStart) {

        this.requestCounter = new AtomicLong(maxRequestsPerIp);
        this.blockCounter = new AtomicInteger(blockCounterStart);
    }

    ///
    public boolean isRateLimited() {

        return this.requestCounter.decrementAndGet() <= 0;
    }

    ///..
    public boolean replenish(final int maxRequestsPerIp) {

        final long requestCounterValue = this.requestCounter.get();
        final int blockCounterValue = this.blockCounter.get();

        if(requestCounterValue == maxRequestsPerIp || (requestCounterValue <= 0 && blockCounterValue == 0)) return true;
        else if(requestCounterValue <= 0 && blockCounterValue > 0) this.blockCounter.decrementAndGet();
        else this.requestCounter.set(maxRequestsPerIp);

        return false;
    }

    ///
}
