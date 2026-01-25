package io.github.clamentos.gattoslab.session.containers;

///
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.session.SessionMetadata;

///.
import java.util.Collection;

///.
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

///
public interface SessionContainer {

    ///
    @NonNull SessionMetadata createSession(@Nullable final String authorization, @NonNull final String fingerprint, @NonNull final boolean forceCreate)
    throws ApiSecurityException;

    ///..
    @Nullable SessionMetadata getSession(@Nullable final String sessionId);
    @NonNull Collection<SessionMetadata> getSessions();

    ///..
    void deleteSession(@Nullable final String sessionId);
    void cleanExpired(final long timestamp);

    ///
}
