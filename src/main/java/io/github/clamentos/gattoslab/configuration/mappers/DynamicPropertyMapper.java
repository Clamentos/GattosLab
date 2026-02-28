package io.github.clamentos.gattoslab.configuration.mappers;

///
import io.github.clamentos.gattoslab.configuration.DynamicPropertyType;

///..
import org.bson.Document;

///
public interface DynamicPropertyMapper {

    ///
    DynamicPropertyType forType();
    Object map(final Document document) throws IllegalArgumentException;

    ///
}
