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
    PATH_INVOCATIONS("PathInvocations"),
    REQUEST_METRICS("RequestMetrics"),
    SYSTEM_METRICS("SystemMetrics"),
    USER_AGENTS("UserAgents");

    ///
    @NonNull private final String value;

    ///
}
