package io.github.clamentos.gattoslab.configuration.dynamic;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class DynamicPropertyEntity<T> {

    ///
    private final DynamicPropertyType key;
    private final T value;

    ///
}
