package io.github.clamentos.gattoslab.exceptions;

///
public final class EarlyTerminationException extends Exception {

    ///
    public EarlyTerminationException(final String message, final String source) {

        super(message, new CauseContainer(source), false, false);
    }

    ///
}
