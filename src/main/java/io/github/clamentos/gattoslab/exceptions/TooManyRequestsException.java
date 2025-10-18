package io.github.clamentos.gattoslab.exceptions;

///
import java.io.IOException;

///.
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class TooManyRequestsException extends IOException {

    ///
    private final String ip;

    ///
}
