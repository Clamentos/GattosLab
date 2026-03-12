package io.github.clamentos.gattoslab.configuration.dynamic.mappers;

///
import io.github.clamentos.gattoslab.configuration.dynamic.DynamicPropertyEntity;
import io.github.clamentos.gattoslab.configuration.dynamic.DynamicPropertyType;

///..
import java.util.EnumMap;
import java.util.Map;

///..
import org.bson.BsonReader;
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
    public DynamicPropertyEntity<?> decode(final BsonReader reader, final DecoderContext decoderContext) {

        reader.readStartDocument();

        final DynamicPropertyType type = DynamicPropertyType.valueOf(reader.readString("key"));
        final Object value = subMappers.get(type).map(reader);

        reader.readEndDocument();

        return new DynamicPropertyEntity<>(type, value);
    }

    ///
}
