package io.github.clamentos.gattoslab.exceptions;

///
public class CodecException extends RuntimeException {

    ///
    public CodecException(final String message) {

        super(message, null, false, false);
    }

    ///..
    public CodecException(final String message, final Throwable cause) {

        super(message, cause, false, false);
    }

    ///
}
