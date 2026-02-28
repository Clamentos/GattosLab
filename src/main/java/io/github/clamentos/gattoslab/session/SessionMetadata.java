package io.github.clamentos.gattoslab.session;

///
import com.fasterxml.jackson.annotation.JsonIgnore;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class SessionMetadata {

    ///
    @JsonIgnore
    private final String sessionId;

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
