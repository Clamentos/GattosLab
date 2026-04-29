package io.github.clamentos.gattoslab.configuration.environments;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
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
    private final BatchConfig batchConfig;
    private final CorsConfig corsConfig;
    private final DatabaseConfig databaseConfig;
    private final DynamicPropertiesConfig dynamicPropertiesConfig;
    private final LogsConfig logsConfig;
    private final MetricsConfig metricsConfig;
    private final RateLimitConfig rateLimitConfig;
    private final SessionAdminConfig sessionAdminConfig;
    private final SessionConfig sessionConfig;
    private final SiteConfig siteConfig;
    private final SslConfig sslConfig;
    private final WebserverConfig webserverConfig;

    ///
    public ProdProperties() throws IllegalArgumentException {

        batchConfig = new BatchConfig(Duration.ofSeconds(10));
        corsConfig = new CorsConfig(true, Set.of("https://gattoslab.dev", "https://www.gattoslab.dev"), Duration.ofHours(1));

        databaseConfig = new DatabaseConfig(

            super.resolve("DB_CONNECTION_STRING", String.class),
            4, 16,
            Duration.ofSeconds(30),
            Duration.ofMinutes(5),
            Duration.ofSeconds(20),
            Duration.ofSeconds(15)
        );

        dynamicPropertiesConfig = new DynamicPropertiesConfig(CommonCrons.EVERY_MINUTE);
        logsConfig = new LogsConfig(Duration.ofDays(7), CommonCrons.EVERY_DAY, CommonCrons.EVERY_MINUTE);

        metricsConfig = new MetricsConfig(

            CommonCrons.EVERY_MINUTE,
            true,
            Duration.ofDays(7),
            CommonCrons.EVERY_DAY,
            10000,
            Duration.ofDays(7),
            CommonCrons.EVERY_5_SECONDS,
            CommonCrons.EVERY_SECOND
        );

        rateLimitConfig = new RateLimitConfig(true, 200, CommonCrons.EVERY_10_SECONDS, Duration.ofMinutes(1));

        sessionAdminConfig = new SessionAdminConfig(

            super.resolve("ADMIN_API_KEY", String.class), 10,
            "=$; Secure; HttpOnly; Domain=gattoslab.dev; SameSite=Strict; Path=/;"
        );

        sessionConfig = new SessionConfig(CommonCrons.EVERY_MINUTE, "GattosLabSessionId", Duration.ofHours(1), true, Duration.ofMillis(500));
        siteConfig = new SiteConfig(Duration.ofDays(7), "site");
        sslConfig = new SslConfig(true, super.resolve("SSL_KEY_STORE_PASSWORD", String.class));
        webserverConfig = new WebserverConfig(443, "::");
    }

    ///
}
