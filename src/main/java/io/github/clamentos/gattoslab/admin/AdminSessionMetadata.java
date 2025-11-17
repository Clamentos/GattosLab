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
    private final String fingerprint;
    private final long createdAt;
    private final long expiresAt;

    ///
    public boolean isExpired(final long reference) {

        return expiresAt < reference;
    }

    ///
}
