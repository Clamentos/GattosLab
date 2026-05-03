package io.github.clamentos.gattoslab.exceptions;

///
public final class CauseContainer extends Throwable {

    ///
    public CauseContainer(final String message) {

        super(message, null, false, false);
    }

    ///..
    public CauseContainer(final String message, final Throwable cause) {

        super(message, cause, false, false);
    }

    ///
}
