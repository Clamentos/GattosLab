package io.github.clamentos.gattoslab.configuration.dynamic;

///
import io.github.clamentos.gattoslab.exceptions.CodecException;

///
public enum DynamicPropertyType {

    ///
    BLACKLIST;

    ///
    public static DynamicPropertyType decode(final String value) throws CodecException {

        if("BLACKLIST".equals(value)) return DynamicPropertyType.BLACKLIST;
        throw new CodecException("Unknown type '" + value + "'", "DynamicPropertyType.decode");
    }

    ///
}
