package io.github.clamentos.gattoslab.observability.logging.squash;

///
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

///..
import lombok.extern.slf4j.Slf4j;

///
@Slf4j

///
public final class BlacklistSquash extends RateLimitSquash {

    ///
    @Override
    public SquashLogEventType getType() {

        return SquashLogEventType.BLACKLISTED;
    }

    ///..
    @Override
    public void log() {

        for(final Map.Entry<String, AtomicInteger> squashEntry : super.squashesMap.entrySet()) {

            log.warn("Blocked {} times by blacklist for fingerprint: {}", squashEntry.getValue(), squashEntry.getKey());
        }
    }

    ///
}
