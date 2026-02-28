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

///..
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

        batchConfig = new BatchConfig(5, 10000);
        corsConfig = new CorsConfig(true, Set.of("https://gattoslab.dev", "https://www.gattoslab.dev"), 3600);
        databaseConfig = new DatabaseConfig(super.resolve("DB_CONNECTION_STRING", String.class), 4, 16, 30, 300, 20, 15);
        dynamicPropertiesConfig = new DynamicPropertiesConfig("m1");
        logsConfig = new LogsConfig(7, "h24", "m1");
        metricsConfig = new MetricsConfig("m1", true, 7, "h24", 100000, 7, "s10");
        rateLimitConfig = new RateLimitConfig(true, 200, "s10", 60000);

        sessionAdminConfig = new SessionAdminConfig(

            super.resolve("ADMIN_API_KEY", String.class), 10,
            "=$; Secure; HttpOnly; Domain=gattoslab.dev; SameSite=Strict; Path=/;"
        );

        sessionConfig = new SessionConfig("m1", "GattosLabSessionId", 3600, true, 500);
        siteConfig = new SiteConfig(10080, "site");
        sslConfig = new SslConfig(true, super.resolve("SSL_KEY_STORE_PASSWORD", String.class));
        webserverConfig = new WebserverConfig(443, "::");
    }

    ///
}
