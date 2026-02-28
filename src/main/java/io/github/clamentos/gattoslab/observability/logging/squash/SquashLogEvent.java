package io.github.clamentos.gattoslab.observability.logging.squash;

///
public interface SquashLogEvent {

    ///
    SquashLogEventType getType();

    ///..
    void update(final Object value);
    void log();
    void reset();

    ///
}
