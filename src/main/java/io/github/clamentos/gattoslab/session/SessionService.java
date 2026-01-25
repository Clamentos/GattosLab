package io.github.clamentos.gattoslab.session;

///
import io.github.clamentos.gattoslab.configuration.PropertyProvider;
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.session.containers.AdminSessionContainer;
import io.github.clamentos.gattoslab.session.containers.SessionContainer;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///.
import jakarta.el.PropertyNotFoundException;

///.
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

///..
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

///
@Service
@Slf4j

///
public final class SessionService {

    ///
    private final long loginDelay;

    ///..
    private final Map<SessionRole, SessionContainer> sessionContainers;

    ///..
    private final Lock lock;

    ///
    @Autowired
    public SessionService(@NonNull final PropertyProvider propertyProvider) throws PropertyNotFoundException {

        loginDelay = propertyProvider.getProperty("app.session.loginDelay", Long.class);

        sessionContainers = new EnumMap<>(SessionRole.class);
        for(final SessionRole role : SessionRole.values()) sessionContainers.put(role, new AdminSessionContainer(propertyProvider));

        lock = new ReentrantLock();
    }

    ///
    // Designed to be slow when wrong on purpose.
    public @Nullable SessionMetadata check(@NonNull final SessionRole role, @Nullable final String sessionId, @Nullable final String fingerprint) {

        final SessionMetadata session = sessionContainers.get(role).getSession(sessionId);

        if(session == null || !session.isValid(System.currentTimeMillis(), fingerprint)) {

            lock.lock();
            GenericUtils.sleep(loginDelay);
            lock.unlock();

            return null;
        }

        return session;
    }

    ///..
    public @NonNull SessionMetadata createSession(

        @Nullable final String authorization,
        @NonNull final SessionRole role,
        @Nullable final String ip,
        @Nullable final String userAgent

    ) throws ApiSecurityException {

        return sessionContainers.get(role).createSession(authorization, GenericUtils.composeFingerprint(ip, userAgent), false);
    }

    ///..
    public @NonNull SessionMetadata refreshSession(

        @Nullable final String sessionId,
        @NonNull final SessionRole role,
        @Nullable final String ip,
        @Nullable final String userAgent

    ) throws ApiSecurityException {

        final String fingerprint = GenericUtils.composeFingerprint(ip, userAgent);
        final SessionMetadata session = this.check(role, sessionId, fingerprint);

        if(session != null) {

            final SessionContainer container = sessionContainers.get(role);
            container.deleteSession(sessionId);

            return container.createSession(null, fingerprint, true);
        }

        else {

            throw new ApiSecurityException("Invalid, expired or non existent session");
        }
    }

    ///..
    public void deleteSession(@Nullable final String sessionId) {

        for(final SessionContainer sessionContainer : sessionContainers.values()) {

            sessionContainer.deleteSession(sessionId);
        }
    }

    ///..
    public @NonNull List<SessionMetadata> getSessionsMetadata() {

        final List<SessionMetadata> sessions = new ArrayList<>();
        for(final SessionContainer sessionContainer : sessionContainers.values()) sessions.addAll(sessionContainer.getSessions());

        return sessions;
    }

    ///.
    @Scheduled(cron = "${app.session.cleanSchedule}", scheduler = "batchScheduler")
    protected void cleanExpired() {

        final long now = System.currentTimeMillis();

        for(final SessionContainer sessionContainer : sessionContainers.values()) {

            sessionContainer.cleanExpired(now);
        }
    }

    ///
}
