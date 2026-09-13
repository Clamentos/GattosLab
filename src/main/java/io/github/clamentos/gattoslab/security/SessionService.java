package io.github.clamentos.gattoslab.security;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.FastAsciiJoiner;
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.server.HttpExchange;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

///
public final class SessionService {

    ///
    private final Logger logger;

    ///..
    private final Map<String, Session> sessions;
    private final AtomicInteger sessionCounter;
    private final Random randomness;

    ///..
    private final String loginPassword;
    private final String cookieProperties;

    ///..
    public SessionService(final ApplicationProperties applicationProperties, final BatchScheduler batchScheduler) {

        this.logger = new Logger();

        this.sessions = new ConcurrentHashMap<>();
        this.sessionCounter = new AtomicInteger();

        SecureRandom secureRandom;

        try {

            secureRandom = SecureRandom.getInstanceStrong();
        }

        catch(final NoSuchAlgorithmException exc) {
            
            this.logger.warning("SecureRandom.getInstanceStrong() failed because", exc);
            secureRandom = new SecureRandom();
        }

        this.randomness = secureRandom;

        this.loginPassword = applicationProperties.getLoginPassword();
        this.cookieProperties = applicationProperties.getCookieProperties();

        batchScheduler.schedule(

            this::removeExpiredTask,
            "gattos-lab-session-maintenance-task",
            ApplicationProperties.SESSION_SERVICE_MAINTENANCE_CRON
        );
    }

    ///
    public SecurityFailure login(final HttpExchange exchange) {

        if(!this.loginPassword.equals(exchange.getRequestHeaders().get(HttpHeader.AUTHORIZATION))) {

            GenericUtils.silentSleep(ApplicationProperties.LOGIN_FAILURE_PAUSE_DURATION.toMillis());
            return SecurityFailure.INCORRECT_PASSWORD;
        }

        final int currentSessionCount = this.sessionCounter.getAndUpdate((currentValue -> Math.min(

            currentValue + 1,
            ApplicationProperties.MAX_SESSIONS
        )));

        if(currentSessionCount >= ApplicationProperties.MAX_SESSIONS) return SecurityFailure.TOO_MANY_SESSIONS;

        final byte[] sessionId = new byte[ApplicationProperties.SESSION_ID_SIZE];
        this.randomness.nextBytes(sessionId);

        final Session session = new Session(

            Base64.getEncoder().encodeToString(sessionId),
            GenericUtils.composeFingerprint(exchange),
            System.currentTimeMillis() + ApplicationProperties.SESSION_DURATION.toMillis()
        );

        exchange.getResponseHeaders().put(

            HttpHeader.SET_COOKIE,
            (ApplicationProperties.SESSION_COOKIE_NAME + "=" + session.getSessionId() + this.cookieProperties).getBytes()
        );

        this.sessions.put(session.getSessionId(), session);
        return null;
    }

    ///..
    public SecurityFailure isAllowed(final List<String> cookies) {

        if(cookies == null || cookies.isEmpty()) return SecurityFailure.INVALID_COOKIE_HEADER;

        final String sessionId = this.extractSessionIdCookie(cookies);
        if(sessionId == null) return SecurityFailure.NO_COOKIE_FOUND;

        final Session session = this.sessions.get(sessionId);

        if(session == null) return SecurityFailure.NO_SESSION_FOUND;
        if(session.getExpiresAt() < System.currentTimeMillis()) return SecurityFailure.EXPIRED_SESSION;

        return null;
    }

    ///..
    public void logout(final List<String> cookies) {

        if(this.sessions.remove(this.extractSessionIdCookie(cookies)) != null) {

            this.sessionCounter.decrementAndGet();
        }
    }

    ///..
    public byte[] getSessions() {

        final FastAsciiJoiner joiner = new FastAsciiJoiner(this.sessions.values().size() << 2);

        for(final Session session : this.sessions.values()) {

            session.appendBytes(joiner);
            joiner.add("\n");
        }

        joiner.deleteLast();
        return joiner.toByteArray();
    }

    ///.
    private String extractSessionIdCookie(final List<String> cookies) {

        final int length = cookies.size();

        for(int i = 0; i < length; i++) {

            final String cookie = cookies.get(i);

            if(cookie.contains(ApplicationProperties.SESSION_COOKIE_NAME)) {

                return cookie.substring(ApplicationProperties.SESSION_COOKIE_NAME.length() + 1);
            }
        }

        return null;
    }

    ///..
    private void removeExpiredTask() {

        final long now = System.currentTimeMillis();
        final int initialSize = this.sessionCounter.get();
        final Iterator<Entry<String, Session>> iterator = this.sessions.entrySet().iterator();

        while(iterator.hasNext()) {

            if(iterator.next().getValue().getExpiresAt() < now) iterator.remove();
        }

        final int newSize = this.sessions.size();
        final int sizeDifference = initialSize - newSize;

        this.sessionCounter.set(newSize);
        if(sizeDifference > 0) this.logger.info("Removed " + sizeDifference + " expired sessions");
    }

    ///
}
