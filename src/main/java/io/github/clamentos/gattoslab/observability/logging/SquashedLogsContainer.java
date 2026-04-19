package io.github.clamentos.gattoslab.observability.logging;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.observability.logging.squash.SquashLogEvent;
import io.github.clamentos.gattoslab.observability.logging.squash.SquashLogEventType;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;

///..
import java.io.Closeable;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

///..
import lombok.extern.slf4j.Slf4j;

///
@Slf4j

///
public final class SquashedLogsContainer implements Closeable {

    ///
    private final Map<SquashLogEventType, SquashLogEvent> squashEvents;

    ///
    public SquashedLogsContainer(final ApplicationProperties applicationProperties, final BatchScheduler batchScheduler, final List<SquashLogEvent> squashEvents)
    throws IllegalArgumentException {

        batchScheduler.schedule(this::log, "SquashedLogContainer::log", applicationProperties.getLogsConfig().getSquashSchedule());

        this.squashEvents = new EnumMap<>(SquashLogEventType.class);
        for(final SquashLogEvent squashEvent : squashEvents) this.squashEvents.put(squashEvent.getType(), squashEvent);
    }

    ///
    public <T> void squash(final SquashLogEventType eventType, final T value) {

        squashEvents.get(eventType).update(value);
    }

    ///..
    public void close() {

        log.info("Begin shutdown...");
        this.log();
        log.info("End shutdown");
    }

    ///.
    private void log() {

        for(final SquashLogEvent event : squashEvents.values()) {

            event.log();
            event.reset();
        }
    }

    ///
}
