package io.github.clamentos.gattoslab.security.components;

///
import java.nio.charset.StandardCharsets;

///..
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter

///
public enum SecurityFailure {

    ///
    EXPIRED_SESSION("Expired session".getBytes(StandardCharsets.ISO_8859_1)),
    INCORRECT_PASSWORD("Incorrect password".getBytes(StandardCharsets.ISO_8859_1)),
    INVALID_COOKIE_HEADER("Valid 'Cookie' header must be provided".getBytes(StandardCharsets.ISO_8859_1)),
    NO_COOKIE_FOUND("Session cookie must be provided".getBytes(StandardCharsets.ISO_8859_1)),
    NO_SESSION_FOUND("No session found".getBytes(StandardCharsets.ISO_8859_1)),
    TOO_MANY_SESSIONS("Too many sessions".getBytes(StandardCharsets.ISO_8859_1));

    ///
    private final byte[] message;

    ///
}
