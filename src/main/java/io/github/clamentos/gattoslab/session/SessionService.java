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
    public SessionService(final PropertyProvider propertyProvider) throws PropertyNotFoundException {

        loginDelay = propertyProvider.getProperty("app.session.loginDelay", Long.class);

        sessionContainers = new EnumMap<>(SessionRole.class);
        for(final SessionRole role : SessionRole.values()) sessionContainers.put(role, new AdminSessionContainer(propertyProvider));

        lock = new ReentrantLock();
    }

    ///
    // Designed to be slow when wrong on purpose.
    public boolean check(final SessionRole role, final String sessionId, final String fingerprint) {

        final SessionMetadata session = sessionContainers.get(role).getSession(sessionId);

        if(session == null || !session.isValid(System.currentTimeMillis(), fingerprint)) {

            lock.lock();
            GenericUtils.sleep(loginDelay);
            lock.unlock();

            return false;
        }

        return true;
    }

    ///..
    public String createSession(final String authorization, final SessionRole role, final String ip, final String userAgent)
    throws ApiSecurityException {

        return sessionContainers.get(role).createSession(authorization, GenericUtils.composeFingerprint(ip, userAgent));
    }

    ///..
    public void deleteSession(final String sessionId) {

        if(sessionId != null) {

            for(final SessionContainer sessionContainer : sessionContainers.values()) sessionContainer.deleteSession(sessionId);
        }
    }

    ///..
    public List<SessionMetadata> getSessionsMetadata() {

        final List<SessionMetadata> sessions = new ArrayList<>();
        for(final SessionContainer sessionContainer : sessionContainers.values()) sessions.addAll(sessionContainer.getSessions());

        return sessions;
    }

    ///.
    @Scheduled(cron = "${app.session.cleanSchedule}", scheduler = "batchScheduler")
    protected void cleanExpired() {

        final long now = System.currentTimeMillis();
        for(final SessionContainer sessionContainer : sessionContainers.values()) sessionContainer.cleanExpired(now);
    }

    ///
}
