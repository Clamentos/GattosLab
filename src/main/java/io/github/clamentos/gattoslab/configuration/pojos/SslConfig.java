package io.github.clamentos.gattoslab.configuration.pojos;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class SslConfig {

    ///
    private final boolean enabled;
    private final String keystorePassword;

    ///
}
