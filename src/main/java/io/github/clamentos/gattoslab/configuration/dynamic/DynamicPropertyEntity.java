package io.github.clamentos.gattoslab.configuration.dynamic;

///
import io.github.clamentos.gattoslab.utils.Hashable;

///..
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

///
@AllArgsConstructor
@EqualsAndHashCode
@Getter

///
public final class DynamicPropertyEntity<T extends Hashable> implements Hashable {

    ///
    private final DynamicPropertyType key;
    private final boolean enabled;
    private final T value;

    ///
}
