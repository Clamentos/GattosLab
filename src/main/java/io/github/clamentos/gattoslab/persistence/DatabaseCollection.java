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
    CHARTS("Charts"),
    PATHS_INVOCATIONS("PathsInvocations"),
    USER_AGENTS("UserAgents");

    ///
    private final String value;

    ///
}
