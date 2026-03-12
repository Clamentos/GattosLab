package io.github.clamentos.gattoslab.observability.logging.entities;

///
import java.util.List;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class LogEntityStackTrace {

    ///
    private final String className;
    private final String message;
    private final List<String> stacktrace;

    ///
}
