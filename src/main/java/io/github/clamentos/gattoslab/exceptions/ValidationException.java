package io.github.clamentos.gattoslab.exceptions;

///
public final class ValidationException extends Exception {

    ///
    public ValidationException(final String message, final String source) {

        super(message, new CauseContainer(source), false, false);
    }

    ///
}
