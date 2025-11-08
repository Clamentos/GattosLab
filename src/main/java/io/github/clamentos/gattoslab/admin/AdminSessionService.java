package io.github.clamentos.gattoslab.admin;

///
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.utils.PropertyProvider;

///.
import jakarta.el.PropertyNotFoundException;

///.
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
    private final Map<String, AdminSessionMetadata> sessions;
    private final AtomicInteger sizeCounter;

    ///..
    private final Lock lock;
    private final Random random;

    ///..
    private final String apiKey;
    private final long loginSleep;
    private final int sessionDuration;
    private final int maxSessions;

    ///
    @Autowired
    public AdminSessionService(final PropertyProvider propertyProvider) throws PropertyNotFoundException {

        sessions = new ConcurrentHashMap<>();
        sizeCounter = new AtomicInteger();

        lock = new ReentrantLock();
        random = new Random();

        apiKey = propertyProvider.getProperty("app.admin.apiKey", String.class);
        loginSleep = propertyProvider.getProperty("app.admin.loginSleep", Long.class);
        sessionDuration = propertyProvider.getProperty("app.admin.sessionDuration", Integer.class);
        maxSessions = propertyProvider.getProperty("app.admin.maxSessions", Integer.class);
    }

    ///
    // Designed to be slow when wrong on purpose.
    @SuppressWarnings("squid:S2276")
    public boolean check(final String sessionId) {

        if(sessions.get(sessionId) == null) {

            try {

                lock.lock();
                Thread.sleep(loginSleep);
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
    public String createSession(final String apiKey, final String ip) throws ApiSecurityException {

        if(apiKey == null || !apiKey.equals(this.apiKey) || sizeCounter.getAndIncrement() >= maxSessions) {

            throw new ApiSecurityException("Invalid key for ip: " + ip);
        }

        final long now = System.currentTimeMillis();
        final byte[] sessionId = new byte[32];
        random.nextBytes(sessionId);

        final String sessionIdString = new String(Base64.getEncoder().encode(sessionId));
        sessions.put(sessionIdString, new AdminSessionMetadata(ip, now, now + sessionDuration));

        log.info("Admin session created for ip: {}", ip);
        return sessionIdString;
    }

    ///..
    public void deleteSession(final String sessionId, final String ip) {

        if(sessionId != null && sessions.remove(sessionId) != null) {

            sizeCounter.decrementAndGet();
            log.info("Admin session logout for ip: {}", ip);
        }
    }

    ///..
    public Collection<AdminSessionMetadata> getSessionsMetadata() {

        return sessions.values();
    }

    ///.
    @Scheduled(fixedDelayString = "${app.admin.sessionCleanSchedule}")
    protected void cleanExpired() {

        final Iterator<Map.Entry<String, AdminSessionMetadata>> entries = sessions.entrySet().iterator();
        final long now = System.currentTimeMillis();

        while(entries.hasNext()) {

            final Map.Entry<String, AdminSessionMetadata> entry = entries.next();
            final AdminSessionMetadata metadata = entry.getValue();

            if(metadata.getExpiresAt() < now && sessions.remove(entry.getKey()) != null) {

                sizeCounter.decrementAndGet();
                log.info("Admin session expired for ip: {}", metadata.getIp());
            }
        }
    }

    ///
}
