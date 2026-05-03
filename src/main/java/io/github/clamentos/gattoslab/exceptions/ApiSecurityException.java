package io.github.clamentos.gattoslab.exceptions;

///
public final class ApiSecurityException extends Exception {

    ///
    public ApiSecurityException(final String message, final String source) {

        super(message, new CauseContainer(source), false, false);
    }

    ///
}
