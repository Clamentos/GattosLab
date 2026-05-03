package io.github.clamentos.gattoslab.exceptions;

///
public final class IllegalHttpMethodException extends Exception {

    ///
    public IllegalHttpMethodException(final String message, final String source) {

        super(message, new CauseContainer(source), false, false);
    }

    ///
}
