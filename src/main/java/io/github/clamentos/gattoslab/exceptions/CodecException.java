package io.github.clamentos.gattoslab.exceptions;

///
public class CodecException extends RuntimeException {

    ///
    public CodecException(final String message, final String source) {

        super(message, new CauseContainer(source), false, false);
    }

    ///..
    public CodecException(final String message, final String source, final Throwable cause) {

        super(message, new CauseContainer(source, cause), false, false);
    }

    ///
}
