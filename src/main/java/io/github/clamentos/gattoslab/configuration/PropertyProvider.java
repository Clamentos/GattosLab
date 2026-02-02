package io.github.clamentos.gattoslab.configuration;

///
import jakarta.el.PropertyNotFoundException;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

///..
import org.jspecify.annotations.NonNull;

///
@Component

///
public final class PropertyProvider {

    ///
    private final Environment environment;

    ///
    @Autowired
    public PropertyProvider(@NonNull final Environment environment) {

        this.environment = environment;
        System.out.println("DBG " + environment.getProperty("app.database.connectionString"));
    }

    ///
    public @NonNull <T> T getProperty(@NonNull final String key, @NonNull final Class<T> type) throws PropertyNotFoundException {

        final T property = environment.getProperty(key, type);
        if(property == null) throw new PropertyNotFoundException("Property " + key + " is not defined");

        return property;
    }

    ///
}
