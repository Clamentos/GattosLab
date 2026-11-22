package io.github.clamentos.gattoslab.exceptions;

///
import java.io.IOException;

///..
import lombok.Getter;

///
@Getter

///
public final class MalformedRequestException extends IOException {

    ///
    private final byte[] response;

    ///
    public MalformedRequestException(final String message, final byte[] response) {

        super(message);
        this.response = response;
    }

    ///
}
