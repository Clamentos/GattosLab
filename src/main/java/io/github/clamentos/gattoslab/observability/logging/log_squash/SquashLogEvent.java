package io.github.clamentos.gattoslab.observability.logging.log_squash;

///
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

///
public interface SquashLogEvent {

    ///
    @NonNull SquashLogEventType getType();

    ///..
    void update(@Nullable final Object value);
    void log();
    void reset();

    ///
}
