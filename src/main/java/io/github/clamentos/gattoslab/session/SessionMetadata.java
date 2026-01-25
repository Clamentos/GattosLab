package io.github.clamentos.gattoslab.session;

///
import com.fasterxml.jackson.annotation.JsonIgnore;

///.
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

///.
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

///
@AllArgsConstructor
@Getter
@ToString

///
public final class SessionMetadata {

    ///
    @JsonIgnore
    @NonNull private final String sessionId;

    @NonNull private final SessionRole role;
    @NonNull private final String fingerprint;
    private final long createdAt;
    private final long expiresAt;

    ///
    public boolean isValid(final long timestamp, @Nullable final String incomingFingerprint) {

        if(incomingFingerprint != null) return expiresAt >= timestamp && fingerprint.equals(incomingFingerprint);
        else return expiresAt >= timestamp;
    }

    ///
}
