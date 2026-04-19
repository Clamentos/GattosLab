package io.github.clamentos.gattoslab.session.containers;

///
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.session.SessionMetadata;

///..
import java.util.Collection;
import java.util.Map.Entry;

///
public interface SessionContainer {

    ///
    Entry<String, SessionMetadata> createSession(final String authorization, final String fingerprint, final boolean forceCreate) throws ApiSecurityException;

    ///..
    SessionMetadata getSession(final String sessionId);
    Collection<SessionMetadata> getSessions();

    String getCookieAttributes();

    ///..
    void deleteSession(final String sessionId);
    void cleanExpired(final long timestamp);

    ///
}
