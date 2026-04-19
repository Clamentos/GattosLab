package io.github.clamentos.gattoslab.configuration.pojos;

///
import java.time.Duration;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class DatabaseConfig {

    ///
    private final String connectionString;

    private final int minPoolSize;
    private final int maxPoolSize;
    private final Duration maintenanceFrequency;
    private final Duration maxConnectionIdleTime;

    private final Duration connectTimeout;
    private final Duration readTimeout;

    ///
}
