package io.github.clamentos.gattoslab.configuration.dynamic.mappers;

///
import io.github.clamentos.gattoslab.exceptions.CodecException;
import io.github.clamentos.gattoslab.utils.Hashable;

///..
import org.bson.BsonReader;

///
@FunctionalInterface

///
public interface DynamicPropertySubMapper<T extends Hashable> {

    ///
    T map(final BsonReader reader) throws CodecException;

    ///
}
