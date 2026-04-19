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
public final class RateLimitConfig {

    ///
    private final boolean enabled;
    private final int maxTokensPerIp;
    private final String replenishRate;
    private final Duration retryAfter;

    ///
}
