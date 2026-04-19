package io.github.clamentos.gattoslab.configuration.dynamic.mappers;

///
import org.bson.BsonReader;

///
@FunctionalInterface

///
public interface DynamicPropertySubMapper<T> {

    ///
    T map(final BsonReader reader) throws IllegalArgumentException;

    ///
}
