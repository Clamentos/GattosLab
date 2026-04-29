package io.github.clamentos.gattoslab.configuration;

///
import io.github.clamentos.gattoslab.configuration.pojos.BatchConfig;
import io.github.clamentos.gattoslab.configuration.pojos.CorsConfig;
import io.github.clamentos.gattoslab.configuration.pojos.DatabaseConfig;
import io.github.clamentos.gattoslab.configuration.pojos.DynamicPropertiesConfig;
import io.github.clamentos.gattoslab.configuration.pojos.LogsConfig;
import io.github.clamentos.gattoslab.configuration.pojos.MetricsConfig;
import io.github.clamentos.gattoslab.configuration.pojos.RateLimitConfig;
import io.github.clamentos.gattoslab.configuration.pojos.SessionAdminConfig;
import io.github.clamentos.gattoslab.configuration.pojos.SessionConfig;
import io.github.clamentos.gattoslab.configuration.pojos.SiteConfig;
import io.github.clamentos.gattoslab.configuration.pojos.SslConfig;
import io.github.clamentos.gattoslab.configuration.pojos.WebserverConfig;

///
public abstract class ApplicationProperties {

    ///
    public abstract BatchConfig getBatchConfig();
    public abstract CorsConfig getCorsConfig();
    public abstract DatabaseConfig getDatabaseConfig();
    public abstract DynamicPropertiesConfig getDynamicPropertiesConfig();
    public abstract LogsConfig getLogsConfig();
    public abstract MetricsConfig getMetricsConfig();
    public abstract RateLimitConfig getRateLimitConfig();
    public abstract SessionAdminConfig getSessionAdminConfig();
    public abstract SessionConfig getSessionConfig();
    public abstract SiteConfig getSiteConfig();
    public abstract SslConfig getSslConfig();
    public abstract WebserverConfig getWebserverConfig();

    ///
    protected <T> T resolve(final String envName, final Class<T> clazz) throws IllegalArgumentException {

        final String envValue = System.getenv(envName);
        if(envValue == null) throw new IllegalArgumentException("ApplicationProperties.resolve~The environment variable: " + envName + " is not defined");

        if(clazz == String.class) return clazz.cast(envValue);
        if(clazz == Integer.class) return clazz.cast(Integer.parseInt(envValue));
        if(clazz == Long.class) return clazz.cast(Long.parseLong(envValue));
        if(clazz == Boolean.class) return clazz.cast(Boolean.parseBoolean(envValue));

        throw new IllegalArgumentException("ApplicationProperties.resolve~Unknown variable type: " + clazz.getSimpleName());
    }

    ///
}
