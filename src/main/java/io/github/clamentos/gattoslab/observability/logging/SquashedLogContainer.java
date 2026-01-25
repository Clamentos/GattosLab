package io.github.clamentos.gattoslab.observability.logging;

///
import io.github.clamentos.gattoslab.observability.logging.log_squash.SquashLogEvent;
import io.github.clamentos.gattoslab.observability.logging.log_squash.SquashLogEventType;

///.
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

///..
import org.jspecify.annotations.NonNull;

///
@Component
@Slf4j

///
public final class SquashedLogContainer {

    ///
    private final Map<SquashLogEventType, SquashLogEvent> squashEvents;

    ///
    @Autowired
    public SquashedLogContainer(@NonNull final List<SquashLogEvent> squashEvents) {

        this.squashEvents = new EnumMap<>(SquashLogEventType.class);
        for(final SquashLogEvent squashEvent : squashEvents) this.squashEvents.put(squashEvent.getType(), squashEvent);
    }

    ///
    public <T> void squash(@NonNull final SquashLogEventType eventType, @NonNull final T value) {

        squashEvents.get(eventType).update(value);
    }

    ///.
    @Scheduled(cron = "${app.squash.logSchedule}", scheduler = "batchScheduler")
    protected void log() {

        for(final SquashLogEvent event : squashEvents.values()) {

            event.log();
            event.reset();
        }
    }

    ///
}
