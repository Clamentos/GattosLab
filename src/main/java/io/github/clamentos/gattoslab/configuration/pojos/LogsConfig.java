package io.github.clamentos.gattoslab.configuration.pojos;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class LogsConfig {

    ///
    private final int retention;
    private final String retentionSchedule;
    private final String squashSchedule;

    ///
}
