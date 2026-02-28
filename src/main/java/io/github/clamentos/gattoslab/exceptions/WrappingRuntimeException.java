package io.github.clamentos.gattoslab.exceptions;

///
public final class WrappingRuntimeException extends RuntimeException {

    ///
    public WrappingRuntimeException(final Throwable cause) {

        super("Wrapped", cause, false, false);
    }

    ///
}
