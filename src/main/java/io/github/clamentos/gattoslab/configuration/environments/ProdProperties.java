package io.github.clamentos.gattoslab.configuration.environments;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.scheduling.CommonCrons;

///..
import java.time.Duration;
import java.util.Set;

///..
import lombok.Getter;

///
@Getter

///
public final class ProdProperties extends ApplicationProperties {

    ///
    private final Duration batchSchedulerShutdownTimeout;
    private final Duration serverShutdownTimeout;

    ///..
    private final boolean isCorsEnabled;
    private final Set<String> corsAllowedOrigins;
    private final Duration corsMaxAge;

    ///..
    private final String dynamicPropertiesRefreshSchedule;

    ///..
    private final String logsSquashSchedule;

    ///..
    private final String metricsDumpToDbSchedule;
    private final int metricsSiphonCapacity;
    private final String systemMetricsSampling;
    private final String systemMetricsPolling;

    ///..
    private final boolean isRateLimitEnabled;
    private final int rateLimitMaxTokensPerIp;
    private final String rateLimitReplenishRate;
    private final Duration rateLimitRetryAfter;

    ///..
    private final String adminSessionsApiKey;
    private final int adminMaxSessions;
    private final String adminSessionsCookieAttributes;

    ///..
    private final String sessionsCleanSchedule;
    private final String sessionsCookieName;
    private final Duration sessionsDuration;
    private final boolean isSessionsEnabled;
    private final Duration sessionsLoginDelay;

    ///..
    private final Duration siteCacheDuration;
    private final String siteRoot;

    ///..
    private final boolean isSslEnabled;
    private final String sslKeystoreName;
    private final String sslKeystorePassword;

    ///..
    private final int serverPort;
    private final String serverHost;

    ///..
    @Override
    public Environment getCurrentEnvironment() {

        return Environment.PROD;
    }

    ///
    public ProdProperties() throws IllegalArgumentException {

        batchSchedulerShutdownTimeout = Duration.ofSeconds(2);
        serverShutdownTimeout = Duration.ofSeconds(5);

        isCorsEnabled = true;
        corsAllowedOrigins = Set.of("https://gattoslab.dev", "https://www.gattoslab.dev");
        corsMaxAge = Duration.ofDays(7);

        dynamicPropertiesRefreshSchedule = CommonCrons.EVERY_MINUTE;

        logsSquashSchedule = CommonCrons.EVERY_MINUTE;

        metricsDumpToDbSchedule = CommonCrons.EVERY_MINUTE;
        metricsSiphonCapacity = 10000;
        systemMetricsSampling = CommonCrons.EVERY_5_SECONDS;
        systemMetricsPolling = CommonCrons.EVERY_SECOND;

        isRateLimitEnabled = true;
        rateLimitMaxTokensPerIp = 200;
        rateLimitReplenishRate = CommonCrons.EVERY_10_SECONDS;
        rateLimitRetryAfter = Duration.ofMinutes(1);

        adminSessionsApiKey = super.resolve("ADMIN_API_KEY", String.class);
        adminMaxSessions = 10;
        adminSessionsCookieAttributes = "=$; Secure; HttpOnly; Domain=gattoslab.dev; SameSite=Strict; Path=/;";

        sessionsCleanSchedule = CommonCrons.EVERY_MINUTE;
        sessionsCookieName = "GattosLabSessionId";
        sessionsDuration = Duration.ofHours(1);
        isSessionsEnabled = true;
        sessionsLoginDelay = Duration.ofMillis(500);

        siteCacheDuration = Duration.ofDays(7);
        siteRoot = "site";

        isSslEnabled = true;
        sslKeystoreName = "keystore.p12";
        sslKeystorePassword = super.resolve("SSL_KEY_STORE_PASSWORD", String.class);

        serverPort = 443;
        serverHost = "::";
    }

    ///
}
