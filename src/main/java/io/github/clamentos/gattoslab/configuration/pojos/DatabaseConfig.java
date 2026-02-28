package io.github.clamentos.gattoslab.configuration.pojos;

///
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
    private final int maintenanceFrequency;
    private final int maxConnectionIdleTime;

    private final int connectTimeout;
    private final int readTimeout;

    ///
}
