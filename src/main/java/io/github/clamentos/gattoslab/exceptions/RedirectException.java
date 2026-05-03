package io.github.clamentos.gattoslab.exceptions;

///
public final class RedirectException extends Exception {

    ///
    public RedirectException(final String message, final String source) {

        super(message, new CauseContainer(source), false, false);
    }

    ///
}
