package io.github.clamentos.gattoslab.exceptions;

///
import org.jspecify.annotations.Nullable;

///
public final class RedirectException extends Exception {

    ///
    public RedirectException(@Nullable final String message) {

        super(message, null, false, false);
    }

    ///
}
