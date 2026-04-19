package io.github.clamentos.gattoslab.ingress.filters.ratelimit;

///
import java.util.concurrent.atomic.AtomicInteger;

///..
import lombok.Getter;

///
@Getter

///
public final class RateLimitEntry {

    ///
    private final AtomicInteger tokenCounter;
    private final AtomicInteger blockCounter;

    ///
    public RateLimitEntry(final int maxTokensPerIp, final int blockCounterStart) {

        tokenCounter = new AtomicInteger(maxTokensPerIp);
        blockCounter = new AtomicInteger(blockCounterStart);
    }

    ///
    public boolean isRateLimited() {

        return tokenCounter.decrementAndGet() <= 0;
    }

    ///..
    public boolean doReplenish(final int maxTokensPerIp) {

        final int tokenCounterValue = tokenCounter.get();
        final int blockCounterValue = blockCounter.get();

        if(tokenCounterValue == maxTokensPerIp || (tokenCounterValue <= 0 && blockCounterValue == 0)) return true;
        else if(tokenCounterValue <= 0 && blockCounterValue > 0) blockCounter.decrementAndGet();
        else tokenCounter.set(maxTokensPerIp);

        return false;
    }

    ///
}
