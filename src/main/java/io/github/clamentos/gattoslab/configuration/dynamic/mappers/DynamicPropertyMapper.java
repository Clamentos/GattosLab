package io.github.clamentos.gattoslab.configuration.dynamic.mappers;

///
import io.github.clamentos.gattoslab.configuration.dynamic.DynamicPropertyEntity;
import io.github.clamentos.gattoslab.configuration.dynamic.DynamicPropertyType;
import io.github.clamentos.gattoslab.exceptions.CauseContainer;
import io.github.clamentos.gattoslab.exceptions.CodecException;
import io.github.clamentos.gattoslab.persistence.EntityField;
import io.github.clamentos.gattoslab.utils.GenericUtils;
import io.github.clamentos.gattoslab.utils.Hashable;

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
public final class DynamicPropertyMapper implements Codec<DynamicPropertyEntity<? extends Hashable>> {

///
    public static final String SOURCE_DECODE = "DynamicPropertyMapper.decode";

    ///
    private final Map<DynamicPropertyType, DynamicPropertySubMapper<? extends Hashable>> subMappers;

    ///
    public DynamicPropertyMapper() {

        subMappers = new EnumMap<>(DynamicPropertyType.class);
        subMappers.put(DynamicPropertyType.BLACKLIST, new BlacklistMapper());
    }

    ///
    @Override
    @SuppressWarnings("unchecked")
    public Class<DynamicPropertyEntity<? extends Hashable>> getEncoderClass() {

        return (Class<DynamicPropertyEntity<? extends Hashable>>) (Class<? extends Hashable>) DynamicPropertyEntity.class;
    }

    ///..
    @Override
    public void encode(final BsonWriter writer, final DynamicPropertyEntity<? extends Hashable> value, final EncoderContext encoderContext) throws UnsupportedOperationException {

        throw new UnsupportedOperationException(

            "Dynamic properties are currently read-only and must be inserted into the database manually",
            new CauseContainer("DynamicPropertyMapper.encode")
        );
    }

    ///..
    @Override
    public DynamicPropertyEntity<? extends Hashable> decode(final BsonReader reader, final DecoderContext decoderContext) throws CodecException {

        try {

            DynamicPropertyType type = null;
            Boolean enabled = false;
            Hashable value = null;

            reader.readStartDocument();

            while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

                final String name = reader.readName();

                switch(name) {

                    case EntityField.KEY: type = DynamicPropertyType.decode(GenericUtils.readString(reader)); break;

                    case EntityField.ENABLED: 

                        enabled = GenericUtils.readBoolean(reader);
                        if(enabled == null) throw new CodecException("Field 'enabled' cannot be null", SOURCE_DECODE);

                    break;

                    case EntityField.VALUE: value = subMappers.get(type).map(reader); break;

                    case EntityField.ID: reader.readObjectId(); break;
                    default: throw new CodecException("Unknown field '" + name + "'", SOURCE_DECODE);
                }
            }

            reader.readEndDocument();
            return new DynamicPropertyEntity<>(type, enabled, value);
        }

        catch(final IllegalStateException exc) {

            throw new CodecException(GenericUtils.WRAPPED_EXCEPTION_MSG, SOURCE_DECODE, exc);
        }
    }

    ///
}
