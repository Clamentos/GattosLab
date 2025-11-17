package io.github.clamentos.gattoslab.admin;

///
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.utils.GenericUtils;
import io.github.clamentos.gattoslab.utils.PropertyProvider;

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
public final class AdminSessionService {

    ///
    private final String apiKey;
    private final long loginDelay;
    private final long sessionDuration;
    private final int maxSessions;

    ///..
    private final Map<String, AdminSessionMetadata> sessions;
    private final AtomicInteger sizeCounter;

    ///..
    private final Lock lock;
    private final Random random;

    ///
    @Autowired
    public AdminSessionService(final PropertyProvider propertyProvider) throws PropertyNotFoundException {

        apiKey = propertyProvider.getProperty("app.admin.apiKey", String.class);
        loginDelay = propertyProvider.getProperty("app.admin.loginDelay", Long.class);
        sessionDuration = propertyProvider.getProperty("app.admin.sessionDuration", Long.class) * 1000L;
        maxSessions = propertyProvider.getProperty("app.admin.maxSessions", Integer.class);

        sessions = new ConcurrentHashMap<>();
        sizeCounter = new AtomicInteger();

        lock = new ReentrantLock();
        random = new SecureRandom();
    }

    ///
    // Designed to be slow when wrong on purpose.
    public boolean check(final String sessionId) {

        final AdminSessionMetadata session = sessions.get(sessionId);

        if(session == null || session.isExpired(System.currentTimeMillis())) {

            try {

                lock.lock();
                Thread.sleep(loginDelay);
            }

            catch(final InterruptedException exc) {

                Thread.currentThread().interrupt();
                log.warn("Interrupted while sleeping", exc);
            }

            lock.unlock();
            return false;
        }

        return true;
    }

    ///..
    public String createSession(final String apiKey, final String ip, final String userAgent) throws ApiSecurityException {

        final String fingerprint = GenericUtils.composeFingerprint(ip, userAgent);

        if(apiKey == null || !apiKey.equals(this.apiKey)) {

            if(sizeCounter.getAndIncrement() >= maxSessions) throw new ApiSecurityException("Too many sessions");
            else throw new ApiSecurityException("Invalid key for fingerprint: " + fingerprint);
        }

        final long now = System.currentTimeMillis();
        final byte[] sessionId = new byte[32];
        random.nextBytes(sessionId);

        final String sessionIdString = new String(Base64.getEncoder().encode(sessionId));
        sessions.put(sessionIdString, new AdminSessionMetadata(ip, now, now + sessionDuration));

        log.info("Admin session created for fingerprint: {}", fingerprint);
        return sessionIdString;
    }

    ///..
    public void deleteSession(final String sessionId) {

        this.removeSession(sessionId, "Admin session logout for fingerprint");
    }

    ///..
    public Collection<AdminSessionMetadata> getSessionsMetadata() {

        return sessions.values();
    }

    ///.
    @Scheduled(cron = "${app.admin.sessionCleanSchedule}")
    protected void cleanExpired() {

        final long now = System.currentTimeMillis();
        final Iterator<Map.Entry<String, AdminSessionMetadata>> entries = sessions.entrySet().iterator();

        while(entries.hasNext()) {

            final Map.Entry<String, AdminSessionMetadata> entry = entries.next();
            if(entry.getValue().isExpired(now)) this.removeSession(entry.getKey(), "Admin session expired for fingerprint");
        }
    }

    ///.
    private void removeSession(final String sessionId, final String message) {

        if(sessionId != null && sessions.remove(sessionId) != null) {

            final AdminSessionMetadata session = sessions.remove(sessionId);

            if(session != null) {

                sizeCounter.decrementAndGet();
                log.info("{}: {}", message, session.getFingerprint());
            }
        }
    }

    ///
}
