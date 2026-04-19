package io.github.clamentos.gattoslab.configuration.dynamic.mappers;

///
import io.github.clamentos.gattoslab.configuration.dynamic.DynamicPropertyEntity;
import io.github.clamentos.gattoslab.configuration.dynamic.DynamicPropertyType;
import io.github.clamentos.gattoslab.persistence.EntityField;

///..
import java.util.EnumMap;
import java.util.Map;

///..
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

///
public final class DynamicPropertyMapper implements Codec<DynamicPropertyEntity<?>> {

    ///
    private final Map<DynamicPropertyType, DynamicPropertySubMapper<?>> subMappers;

    ///
    public DynamicPropertyMapper() {

        subMappers = new EnumMap<>(DynamicPropertyType.class);
        subMappers.put(DynamicPropertyType.BLACKLIST, new BlacklistMapper());
    }

    ///
    @Override
    @SuppressWarnings("unchecked")
    public Class<DynamicPropertyEntity<?>> getEncoderClass() {

        return (Class<DynamicPropertyEntity<?>>) (Class<?>) DynamicPropertyEntity.class;
    }

    ///..
    @Override
    public void encode(final BsonWriter writer, final DynamicPropertyEntity<?> value, final EncoderContext encoderContext) {

        throw new UnsupportedOperationException("Dynamic properties are currently read-only and must be inserted into the database manually");
    }

    ///..
    @Override
    public DynamicPropertyEntity<?> decode(final BsonReader reader, final DecoderContext decoderContext) throws IllegalArgumentException {

        DynamicPropertyType type = null;
        Object value = null;

        reader.readStartDocument();

        while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

            final String name = reader.readName();

            switch(name) {

                case EntityField.KEY: type = DynamicPropertyType.valueOf(reader.readString()); break;
                case EntityField.VALUE: value = subMappers.get(type).map(reader); break;

                case EntityField.ID: break;
                default: throw new IllegalArgumentException("Unknown field name " + name);
            }
        }

        reader.readEndDocument();
        return new DynamicPropertyEntity<>(type, value);
    }

    ///
}
