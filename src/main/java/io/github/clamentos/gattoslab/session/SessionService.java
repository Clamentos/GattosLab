package io.github.clamentos.gattoslab.session;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.session.containers.AdminSessionContainer;
import io.github.clamentos.gattoslab.session.containers.SessionContainer;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

///..
import lombok.extern.slf4j.Slf4j;

///
@Slf4j

///
public final class SessionService {

    ///
    private final long loginDelay;

    ///..
    private final Map<SessionRole, SessionContainer> sessionContainers;

    ///
    public SessionService(final ApplicationProperties applicationProperties, final BatchScheduler batchScheduler) throws IllegalArgumentException {

        loginDelay = applicationProperties.getSessionsLoginDelay().toMillis();
        batchScheduler.schedule(this::cleanExpired, "SessionService::cleanExpired", applicationProperties.getSessionsCleanSchedule());

        sessionContainers = new EnumMap<>(SessionRole.class);
        for(final SessionRole role : SessionRole.values()) sessionContainers.put(role, new AdminSessionContainer(applicationProperties));
    }

    ///
    public String getCookieAttributes(final SessionRole role) {

        return sessionContainers.get(role).getCookieAttributes();
    }

    ///..
    // Designed to be slow when wrong on purpose.
    public SessionMetadata check(final SessionRole role, final String sessionId, final String fingerprint) {

        final SessionMetadata session = sessionContainers.get(role).getSession(sessionId);

        if(session == null || !session.isValid(System.currentTimeMillis(), fingerprint)) {

            GenericUtils.silentSleep(loginDelay);
            return null;
        }

        return session;
    }

    ///..
    public final Entry<String, SessionMetadata> createSession(

        final String authorization,
        final String sessionId,
        final SessionRole role,
        final InetAddress ip,
        final String userAgent

    ) throws ApiSecurityException {

        final String fingerprint = GenericUtils.composeFingerprint(ip, userAgent);

        if(sessionId != null) {

            final SessionMetadata existingSessionMaybe = this.check(role, sessionId, fingerprint);
            if(existingSessionMaybe != null) return Map.entry(sessionId, existingSessionMaybe);
        }

        return sessionContainers.get(role).createSession(authorization, GenericUtils.composeFingerprint(ip, userAgent), false);
    }

    ///..
    public Entry<String, SessionMetadata> refreshSession(final String sessionId, final SessionRole role, final InetAddress ip, final String userAgent)
    throws ApiSecurityException {

        final String fingerprint = GenericUtils.composeFingerprint(ip, userAgent);
        final SessionMetadata session = this.check(role, sessionId, fingerprint);

        if(session == null) throw new ApiSecurityException("Invalid, expired or non existent session", "SessionService.refreshSession");

        final SessionContainer container = sessionContainers.get(role);
        container.deleteSession(sessionId);

        return container.createSession(null, fingerprint, true);
    }

    ///..
    public void deleteSession(final String sessionId, final SessionRole role) {

        sessionContainers.get(role).deleteSession(sessionId);
    }

    ///..
    public List<SessionMetadata> getSessionsMetadata() {

        final List<SessionMetadata> sessions = new ArrayList<>();
        for(final SessionContainer sessionContainer : sessionContainers.values()) sessions.addAll(sessionContainer.getSessions());

        return sessions;
    }

    ///.
    private void cleanExpired() {

        final long now = System.currentTimeMillis();

        for(final SessionContainer sessionContainer : sessionContainers.values()) {

            sessionContainer.cleanExpired(now);
        }
    }

    ///
}
