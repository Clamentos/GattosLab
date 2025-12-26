package io.github.clamentos.gattoslab.session;

///
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

///
@AllArgsConstructor
@Getter
@ToString

///
public final class SessionMetadata {

    ///
    private final SessionRole role;
    private final String fingerprint;
    private final long createdAt;
    private final long expiresAt;

    ///
    public boolean isValid(final long timestamp, final String incomingFingerprint) {

        if(incomingFingerprint != null) return expiresAt >= timestamp && fingerprint.equals(incomingFingerprint);
        else return expiresAt >= timestamp;
    }

    ///
}
