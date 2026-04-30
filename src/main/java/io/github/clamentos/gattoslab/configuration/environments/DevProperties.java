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
public final class DevProperties extends ApplicationProperties {

    ///
    private final Duration batchSchedulerShutdownTimeout;

    ///..
    private final boolean isCorsEnabled;
    private final Set<String> corsAllowedOrigins;
    private final Duration corsMaxAge;

    ///..
    private final String dbConnectionString;
    private final int dbMinPoolSize;
    private final int dbMaxPoolSize;
    private final Duration dbMaintenanceFrequency;
    private final Duration dbMaxConnectionIdleTime;
    private final Duration dbConnectTimeout;
    private final Duration dbReadTimeout;

    ///..
    private final String dynamicPropertiesRefreshSchedule;

    ///..
    private final Duration logsRetention;
    private final String logsRetentionSchedule;
    private final String logsSquashSchedule;

    ///..
    private final String metricsDumpToDbSchedule;
    private final boolean isRequestMetricsEnabled;
    private final Duration requestMetricsRetention;
    private final String metricsRetentionSchedule;
    private final int metricsSiphonCapacity;
    private final Duration systemMetricsRetention;
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
    private final String sslKeystorePassword;

    ///..
    private final int serverPort;
    private final String serverHost;

    ///
    public DevProperties() {

        batchSchedulerShutdownTimeout = Duration.ofSeconds(10);

        isCorsEnabled = true;
        corsAllowedOrigins = Set.of("https://localhost:8443");
        corsMaxAge = Duration.ofHours(1);

        dbConnectionString = "mongodb://localhost:27017/GattosLabMongoDB";
        dbMinPoolSize = 4;
        dbMaxPoolSize = 16;
        dbMaintenanceFrequency = Duration.ofSeconds(30);
        dbMaxConnectionIdleTime = Duration.ofMinutes(5);
        dbConnectTimeout = Duration.ofSeconds(20);
        dbReadTimeout = Duration.ofSeconds(15);

        dynamicPropertiesRefreshSchedule = CommonCrons.EVERY_MINUTE;

        logsRetention = Duration.ofDays(7);
        logsRetentionSchedule = CommonCrons.EVERY_DAY;
        logsSquashSchedule = CommonCrons.EVERY_MINUTE;

        metricsDumpToDbSchedule = CommonCrons.EVERY_MINUTE;
        isRequestMetricsEnabled = true;
        requestMetricsRetention = Duration.ofDays(7);
        metricsRetentionSchedule = CommonCrons.EVERY_DAY;
        metricsSiphonCapacity = 1000;
        systemMetricsRetention = Duration.ofDays(7);
        systemMetricsSampling = CommonCrons.EVERY_5_SECONDS;
        systemMetricsPolling = CommonCrons.EVERY_SECOND;

        isRateLimitEnabled = true;
        rateLimitMaxTokensPerIp = 200;
        rateLimitReplenishRate = CommonCrons.EVERY_10_SECONDS;
        rateLimitRetryAfter = Duration.ofMinutes(1);

        adminSessionsApiKey = "test";
        adminMaxSessions = 10;
        adminSessionsCookieAttributes = "=$; Path=/;";

        sessionsCleanSchedule = CommonCrons.EVERY_MINUTE;
        sessionsCookieName = "GattosLabSessionId";
        sessionsDuration = Duration.ofHours(1);
        isSessionsEnabled = true;
        sessionsLoginDelay = Duration.ofMillis(500);

        siteCacheDuration = Duration.ofDays(7);
        siteRoot = "site";

        isSslEnabled = true;
        sslKeystorePassword = "password";

        serverPort = 8443;
        serverHost = "localhost";
    }

    ///
}
