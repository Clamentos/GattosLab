package io.github.clamentos.gattoslab.session.containers;

///
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.session.SessionMetadata;
import io.github.clamentos.gattoslab.utils.Pair;

///.
import java.util.Collection;

///
public interface SessionContainer {

    ///
    Pair<String, Long> createSession(final String authorization, final String fingerprint, final boolean forceCreate) throws ApiSecurityException;

    ///..
    Pair<String, SessionMetadata> getSession(final String sessionId);
    Collection<SessionMetadata> getSessions();

    ///..
    void deleteSession(final String sessionId);
    void cleanExpired(final long timestamp);

    ///
}
