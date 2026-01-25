package io.github.clamentos.gattoslab.observability.logging.log_squash;

///
import org.jspecify.annotations.NonNull;

///
public interface SquashLogEvent {

    ///
    @NonNull SquashLogEventType getType();

    ///..
    void update(@NonNull final Object value);
    void log();
    void reset();

    ///
}
