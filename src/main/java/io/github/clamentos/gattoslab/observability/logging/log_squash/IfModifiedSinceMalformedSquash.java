package io.github.clamentos.gattoslab.observability.logging.log_squash;

///
import java.util.concurrent.atomic.AtomicInteger;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.stereotype.Component;

///
@Component
@Slf4j

///
public final class IfModifiedSinceMalformedSquash implements SquashLogEvent {

    ///
    private final AtomicInteger counter;

    ///
    public IfModifiedSinceMalformedSquash() {

        counter = new AtomicInteger();
    }

    ///
    @Override
    public SquashLogEventType getType() {

        return SquashLogEventType.IF_MODIFIED_SINCE_HEADER_MALFORMED;
    }

    ///..
    @Override
    public void update(final Object value) {

        counter.incrementAndGet();
    }

    ///..
    @Override
    public void log() {

        final int counterValue = counter.getAndSet(0);
        if(counterValue > 0) log.warn("Date-time parse exception for header If-Modified-Since: {} times", counterValue);
    }

    ///..
    @Override
    public void reset() {

        counter.set(0);
    }

    ///
}
