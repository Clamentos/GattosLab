package io.github.clamentos.gattoslab.persistence;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///.
import org.jspecify.annotations.NonNull;

///
@AllArgsConstructor
@Getter

///
public enum DatabaseCollection {

    ///
    LOGS("Logs"),
    REQUEST_METRICS("RequestMetrics"),
    SYSTEM_METRICS("SystemMetrics");

    ///
    @NonNull private final String value;

    ///
}
