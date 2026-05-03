package io.github.clamentos.gattoslab.configuration;

import io.github.clamentos.gattoslab.exceptions.CauseContainer;

///
import java.time.Duration;
import java.util.Set;

///
public abstract class ApplicationProperties {

    ///
    private static final String SOURCE_RESOLVE = "ApplicationProperties.resolve";

    ///
    public abstract Duration getBatchSchedulerShutdownTimeout();

    ///..
    public abstract boolean isCorsEnabled();
    public abstract Set<String> getCorsAllowedOrigins();
    public abstract Duration getCorsMaxAge();

    ///..
    public abstract String getDbConnectionString();
    public abstract int getDbMinPoolSize();
    public abstract int getDbMaxPoolSize();
    public abstract Duration getDbMaintenanceFrequency();
    public abstract Duration getDbMaxConnectionIdleTime();
    public abstract Duration getDbConnectTimeout();
    public abstract Duration getDbReadTimeout();

    ///..
    public abstract String getDynamicPropertiesRefreshSchedule();

    ///..
    public abstract Duration getLogsRetention();
    public abstract String getLogsRetentionSchedule();
    public abstract String getLogsSquashSchedule();

    ///..
    public abstract String getMetricsDumpToDbSchedule();
    public abstract boolean isRequestMetricsEnabled();
    public abstract Duration getRequestMetricsRetention();
    public abstract String getMetricsRetentionSchedule();
    public abstract int getMetricsSiphonCapacity();
    public abstract Duration getSystemMetricsRetention();
    public abstract String getSystemMetricsSampling();
    public abstract String getSystemMetricsPolling();

    ///..
    public abstract boolean isRateLimitEnabled();
    public abstract int getRateLimitMaxTokensPerIp();
    public abstract String getRateLimitReplenishRate();
    public abstract Duration getRateLimitRetryAfter();

    ///..
    public abstract String getAdminSessionsApiKey();
    public abstract int getAdminMaxSessions();
    public abstract String getAdminSessionsCookieAttributes();

    ///..
    public abstract String getSessionsCleanSchedule();
    public abstract String getSessionsCookieName();
    public abstract Duration getSessionsDuration();
    public abstract boolean isSessionsEnabled();
    public abstract Duration getSessionsLoginDelay();

    ///..
    public abstract Duration getSiteCacheDuration();
    public abstract String getSiteRoot();

    ///..
    public abstract boolean isSslEnabled();
    public abstract String getSslKeystorePassword();

    ///..
    public abstract int getServerPort();
    public abstract String getServerHost();

    ///
    protected <T> T resolve(final String envName, final Class<T> clazz) throws IllegalArgumentException {

        final String envValue = System.getenv(envName);
        if(envValue == null) throw new IllegalArgumentException("The environment variable '" + envName + "' is not defined", new CauseContainer(SOURCE_RESOLVE));

        if(clazz == String.class) return clazz.cast(envValue);
        if(clazz == Integer.class) return clazz.cast(Integer.parseInt(envValue));
        if(clazz == Long.class) return clazz.cast(Long.parseLong(envValue));
        if(clazz == Boolean.class) return clazz.cast(Boolean.parseBoolean(envValue));

        throw new IllegalArgumentException("Unknown variable type '" + clazz.getSimpleName() + "'", new CauseContainer(SOURCE_RESOLVE));
    }

    ///
}
