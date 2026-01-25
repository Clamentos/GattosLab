package io.github.clamentos.gattoslab.exceptions;

///
import org.jspecify.annotations.Nullable;

///
public final class ApiSecurityException extends Exception {

    ///
    public ApiSecurityException(@Nullable final String message) {

        super(message, null, false, false);
    }

    ///
}
