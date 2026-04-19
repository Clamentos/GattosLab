package io.github.clamentos.gattoslab.configuration.pojos;

///
import java.time.Duration;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class LogsConfig {

    ///
    private final Duration retention;
    private final String retentionSchedule;
    private final String squashSchedule;

    ///
}
