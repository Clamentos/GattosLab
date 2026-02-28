package io.github.clamentos.gattoslab.configuration;

///
import io.github.clamentos.gattoslab.configuration.environments.DevProperties;
import io.github.clamentos.gattoslab.configuration.environments.ProdProperties;

///..
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

///
@Getter
@Slf4j

///
public final class ProfileResolver {

    ///
    private final ApplicationProperties applicationProperties;

    ///
    public ProfileResolver(final String profile) throws IllegalArgumentException {

        final boolean isProd = "prod".equals(profile);

        log.info("Using profile {}", isProd ? "prod" : "dev");
        applicationProperties = isProd ? new ProdProperties() : new DevProperties();
    }

    ///
}
