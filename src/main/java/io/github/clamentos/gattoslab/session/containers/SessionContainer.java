package io.github.clamentos.gattoslab.session.containers;

///
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.session.SessionMetadata;

///.
import java.util.Collection;

///
public interface SessionContainer {

    ///
    String createSession(final String authorization, final String fingerprint) throws ApiSecurityException;

    ///..
    SessionMetadata getSession(final String sessionId);
    Collection<SessionMetadata> getSessions();

    ///..
    void deleteSession(final String sessionId);
    void cleanExpired(final long timestamp);

    ///
}
