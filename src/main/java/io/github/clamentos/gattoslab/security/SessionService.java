package io.github.clamentos.gattoslab.security;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.MutableString;
import io.github.clamentos.gattoslab.datastructures.Pair;
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.server.HttpExchange;
import io.github.clamentos.gattoslab.http.server.ResponseBodyCallback;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

///
public final class SessionService {

    ///
    private final Logger logger;
    private final SquashingLogger squashingLogger;

    ///..
    private final Map<MutableString, Session> sessions;
    private final AtomicInteger sessionCounter;
    private final Random randomness;

    ///..
    private final byte[] loginPassword;
    private final String cookieProperties;

    ///..
    public SessionService(

        final SquashingLogger squashingLogger,
        final ApplicationProperties applicationProperties,
        final BatchScheduler batchScheduler
    ) {

        this.logger = new Logger();
        this.squashingLogger = squashingLogger;

        this.sessions = new ConcurrentHashMap<>();
        this.sessionCounter = new AtomicInteger();

        SecureRandom secureRandom;

        try {

            secureRandom = SecureRandom.getInstanceStrong();
        }

        catch(final NoSuchAlgorithmException exc) {
            
            this.logger.warning("SecureRandom.getInstanceStrong() failed", exc);
            secureRandom = new SecureRandom();
        }

        this.randomness = secureRandom;

        this.loginPassword = applicationProperties.getLoginPassword().getBytes();
        this.cookieProperties = applicationProperties.getCookieProperties();

        batchScheduler.schedule(

            this::removeExpiredTask,
            "gattos-lab-session-maintenance-task",
            ApplicationProperties.SESSION_SERVICE_MAINTENANCE_CRON
        );
    }

    ///
    public Pair<SecurityFailure, Long> login(final HttpExchange exchange) {

        final String authorizationHeader = exchange.getRequestHeaders().get(HttpHeader.AUTHORIZATION);

        if(authorizationHeader == null || !MessageDigest.isEqual(authorizationHeader.getBytes(), this.loginPassword)) {

            this.squashingLogger.warning(GenericUtils.composeMessageForSquash("Login failed for", exchange));
            GenericUtils.silentSleep(ApplicationProperties.LOGIN_FAILURE_PAUSE_DURATION.toMillis());

            return new Pair<>(SecurityFailure.INCORRECT_PASSWORD, null);
        }

        final int currentSessionCount = this.sessionCounter.getAndUpdate((currentValue -> Math.min(

            currentValue + 1,
            ApplicationProperties.MAX_SESSIONS
        )));

        if(currentSessionCount >= ApplicationProperties.MAX_SESSIONS) {

            this.squashingLogger.warning(GenericUtils.composeMessageForSquash("Too many sessions tripped"));
            return new Pair<>(SecurityFailure.TOO_MANY_SESSIONS, null);
        }

        final long duration = ApplicationProperties.SESSION_DURATION.toMillis();
        final long expiration = System.currentTimeMillis() + duration;
        final byte[] sessionId = new byte[ApplicationProperties.SESSION_ID_SIZE];

        this.randomness.nextBytes(sessionId);

        final Session session = new Session(

            new MutableString(Base64.getEncoder().encode(sessionId)),
            GenericUtils.composeFingerprint(exchange),
            expiration
        );

        exchange.getResponseHeaders().put(

            HttpHeader.SET_COOKIE,
            (ApplicationProperties.SESSION_COOKIE_NAME + "=" + session.getSessionId() + this.cookieProperties + " Max-Age=" + (duration / 1000))
        );

        this.sessions.put(session.getSessionId(), session);
        this.logger.info("Login successfull for " + GenericUtils.composeFingerprint(exchange));

        return new Pair<>(null, expiration);
    }

    ///..
    public SecurityFailure isAllowed(final HttpExchange exchange) {

        final String cookies = exchange.getRequestHeaders().get(HttpHeader.COOKIE);
        if(cookies == null || cookies.isEmpty()) return SecurityFailure.INVALID_COOKIE_HEADER;

        final MutableString sessionId = this.extractSessionIdCookie(cookies);
        if(sessionId == null) return SecurityFailure.NO_COOKIE_FOUND;

        final Session session = this.sessions.get(sessionId);

        if(session == null) return SecurityFailure.NO_SESSION_FOUND;
        if(session.getExpiresAt() < exchange.getStartTime()) return SecurityFailure.EXPIRED_SESSION;

        return null;
    }

    ///..
    public void logout(final HttpExchange exchange) {

        final String cookies = exchange.getRequestHeaders().get(HttpHeader.COOKIE);
        if(cookies == null || cookies.isEmpty()) return;

        if(this.sessions.remove(this.extractSessionIdCookie(exchange.getRequestHeaders().get(HttpHeader.COOKIE))) != null) {

            this.logger.info("Logout successfull for " + GenericUtils.composeFingerprint(exchange));
            this.sessionCounter.decrementAndGet();
        }
    }

    ///..
    public ResponseBodyCallback getSessions() {

        return writer -> {

            for(final Session session : this.sessions.values()) {

                session.stream(writer);
                writer.write('\n');
            }
        };
    }

    ///.
    private MutableString extractSessionIdCookie(final String cookies) {

        final int index = cookies.indexOf(ApplicationProperties.SESSION_COOKIE_NAME + "=");
        if(index == -1) return null;

        final int length = cookies.length();
        final MutableString buffer = new MutableString(64);

        for(int i = index + ApplicationProperties.SESSION_COOKIE_NAME.length() + 1; i < length; i++) {

            final char currentChar = cookies.charAt(i);

            if(currentChar == ';') return buffer;
            else buffer.append(currentChar);
        }

        return buffer;
    }

    ///..
    private void removeExpiredTask() {

        final long now = System.currentTimeMillis();
        int removedCounter = 0;

        for(final Entry<MutableString, Session> entry : this.sessions.entrySet()) {

            if(entry.getValue().getExpiresAt() < now && this.sessions.remove(entry.getKey()) != null) {

                this.sessionCounter.decrementAndGet();
                removedCounter++;
            }
        }

        if(removedCounter > 0) this.logger.info("Removed " + removedCounter + " expired sessions");
    }

    ///
}
