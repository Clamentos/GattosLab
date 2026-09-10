package io.github.clamentos.gattoslab.security;

///
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter

///
public enum SecurityFailure {

    ///
    INCORRECT_PASSWORD("Incorrect password".getBytes()),
    TOO_MANY_SESSIONS("Too many sessions".getBytes()),
    INVALID_COOKIE_HEADER("Valid 'Cookie' header must be provided".getBytes()),
    NO_COOKIE_FOUND("Session cookie must be provided".getBytes()),
    NO_SESSION_FOUND("No session found".getBytes()),
    EXPIRED_SESSION("Expired session".getBytes());

    ///
    private final byte[] message;

    ///
}
