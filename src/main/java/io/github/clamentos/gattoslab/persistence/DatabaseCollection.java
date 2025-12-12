package io.github.clamentos.gattoslab.persistence;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public enum DatabaseCollection {

    ///
    LOGS("Logs"),
    PATHS_INVOCATIONS("PathsInvocations"),
    REQUEST_METRICS("RequestMetrics"),
    SYSTEM_METRICS("SystemMetrics"),
    USER_AGENTS("UserAgents");

    ///
    private final String value;

    ///
}
