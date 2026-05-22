package io.github.clamentos.gattoslab.exceptions;

///
public final class BlacklistedException extends Exception {

    ///
    public BlacklistedException(final String message, final String source) {

        super(message, new CauseContainer(source), false, false);
    }

    ///
}
