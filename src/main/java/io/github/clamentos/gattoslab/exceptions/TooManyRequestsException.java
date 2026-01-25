package io.github.clamentos.gattoslab.exceptions;

///
import org.jspecify.annotations.NonNull;

///
public final class TooManyRequestsException extends Exception {

    ///
    public TooManyRequestsException(@NonNull final String message) {

        super(message, null, false, false);
    }

    ///
}
