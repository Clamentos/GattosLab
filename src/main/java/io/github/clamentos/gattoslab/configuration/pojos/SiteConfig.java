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
public final class SiteConfig {

    ///
    private final Duration cacheDuration;
    private final String root;

    ///
}
