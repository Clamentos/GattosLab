package io.github.clamentos.gattoslab.configuration.pojos;

///
import java.util.Set;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class CorsConfig {

    ///
    private final boolean enabled;
    private final Set<String> allowedOrigins;
    private final int maxAge;

    ///
}
