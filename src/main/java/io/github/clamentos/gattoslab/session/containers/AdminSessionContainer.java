package io.github.clamentos.gattoslab.session.containers;

///
import io.github.clamentos.gattoslab.configuration.PropertyProvider;
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.session.SessionMetadata;
import io.github.clamentos.gattoslab.session.SessionRole;
import io.github.clamentos.gattoslab.utils.Pair;

///.
import jakarta.el.PropertyNotFoundException;

///.
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

///.
import lombok.extern.slf4j.Slf4j;

///
@Slf4j

///
public final class AdminSessionContainer implements SessionContainer {

    ///
    private final String apiKey;
    private final int maxSessions;
    private final long sessionDuration;

    ///..
    private final Map<String, SessionMetadata> sessions;
    private final AtomicInteger sizeCounter;

    ///..
    private final Random random;

    ///
    public AdminSessionContainer(final PropertyProvider propertyProvider) throws PropertyNotFoundException {

        apiKey = propertyProvider.getProperty("app.session.admin.apiKey", String.class);
        sessionDuration = propertyProvider.getProperty("app.session.admin.sessionDuration", Long.class) * 1000L;
        maxSessions = propertyProvider.getProperty("app.session.admin.maxSessions", Integer.class);

        sessions = new ConcurrentHashMap<>();
        sizeCounter = new AtomicInteger();

        random = new SecureRandom();
    }

    ///
    @Override
    public Pair<String, Long> createSession(final String authorization, final String fingerprint, final boolean forceCreate) throws ApiSecurityException {

        if(!forceCreate && !this.apiKey.equals(authorization)) throw new ApiSecurityException("Invalid api key for fingerprint: " + fingerprint);

        if(sizeCounter.getAndUpdate(val -> val < maxSessions ? val + 1 : maxSessions) == maxSessions) {

            throw new ApiSecurityException("Too many sessions");
        }

        final byte[] sessionId = new byte[32];
        random.nextBytes(sessionId);

        final long now = System.currentTimeMillis();
        final String sessionIdString = new String(Base64.getEncoder().encode(sessionId));
        final SessionMetadata session = new SessionMetadata(SessionRole.ADMIN, fingerprint, now, now + sessionDuration);

        sessions.put(sessionIdString, session);
        log.info("Admin session created for fingerprint: {}", fingerprint);

        return new Pair<>(sessionIdString, session.getExpiresAt());
    }

    ///..
    @Override
    public Pair<String, SessionMetadata> getSession(final String sessionId) {

        if(sessionId == null) return null;
        final SessionMetadata metadata = sessions.get(sessionId);

        if(metadata != null) return new Pair<>(sessionId, metadata);
        else return null;
    }

    ///..
    @Override
    public Collection<SessionMetadata> getSessions() {

        return sessions.values();
    }

    ///..
    @Override
    public void deleteSession(final String sessionId) {

        this.removeSession(sessionId, "Admin session logout for fingerprint");
    }

    ///..
    @Override
    public void cleanExpired(final long timestamp) {

        final Iterator<Map.Entry<String, SessionMetadata>> entries = sessions.entrySet().iterator();

        while(entries.hasNext()) {

            final Map.Entry<String, SessionMetadata> entry = entries.next();
            if(!entry.getValue().isValid(timestamp, null)) this.removeSession(entry.getKey(), "Admin session logout for fingerprint");
        }
    }

    ///.
    private void removeSession(final String sessionId, final String message) {

        final SessionMetadata removed = sessions.remove(sessionId);

        if(removed != null) {

            sizeCounter.decrementAndGet();
            log.info("{}: {}", message, removed.getFingerprint());
        }
    }

    ///
}
