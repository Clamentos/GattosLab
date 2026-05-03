package io.github.clamentos.gattoslab.exceptions;

///
public final class TooManyRequestsException extends Exception {

    ///
    public TooManyRequestsException(final String message, final String source) {

        super(message, new CauseContainer(source), false, false);
    }

    ///
}
