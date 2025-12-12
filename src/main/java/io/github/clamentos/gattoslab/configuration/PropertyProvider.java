package io.github.clamentos.gattoslab.configuration;

///
import jakarta.el.PropertyNotFoundException;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

///
@Component

///
public final class PropertyProvider {

    ///
    private final Environment environment;

    ///
    @Autowired
    public PropertyProvider(final Environment environment) {

        this.environment = environment;
    }

    ///
    public <T> T getProperty(final String key, final Class<T> type) throws PropertyNotFoundException {

        final T property = environment.getProperty(key, type);
        if(property == null) throw new PropertyNotFoundException("Property " + key + " is not defined");

        return property;
    }

    ///
}
