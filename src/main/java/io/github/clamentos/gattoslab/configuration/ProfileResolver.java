package io.github.clamentos.gattoslab.configuration;

///
import io.github.clamentos.gattoslab.configuration.environments.DevProperties;
import io.github.clamentos.gattoslab.configuration.environments.Environment;
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

        if(profile != null && !profile.isEmpty()) {

            final Environment environment = Environment.valueOf(profile);

            log.info("Using profile {}", environment);
            applicationProperties = environment == Environment.PROD ? new ProdProperties() : new DevProperties();
        }

        else {

            log.info("No profile provided, defaulting to DEV");
            applicationProperties = new DevProperties();
        }
    }

    ///
}
