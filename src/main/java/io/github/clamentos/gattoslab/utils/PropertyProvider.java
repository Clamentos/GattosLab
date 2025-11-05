package io.github.clamentos.gattoslab.utils;

///
import jakarta.el.PropertyNotFoundException;

///.
import lombok.RequiredArgsConstructor;

///.
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

///
@Component
@RequiredArgsConstructor

///
public final class PropertyProvider {

    ///
    private final Environment environment;

    ///
    public <T> T getProperty(final String key, final Class<T> type) throws PropertyNotFoundException {

        final T property = environment.getProperty(key, type);
        if(property == null) throw new PropertyNotFoundException("Property " + key + " is not defined");

        return property;
    }

    ///
}
