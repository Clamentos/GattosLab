package io.github.clamentos.gattoslab.admin;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class AdminSessionMetadata {

    ///
    private final String ip;
    private final long createdAt;
    private final long expiresAt;

    ///
}
