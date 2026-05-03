package io.github.clamentos.gattoslab.observability.metrics.mappers;

///
import io.github.clamentos.gattoslab.exceptions.CauseContainer;
import io.github.clamentos.gattoslab.exceptions.CodecException;
import io.github.clamentos.gattoslab.observability.metrics.entities.UserAgentAggregationEntity;
import io.github.clamentos.gattoslab.persistence.EntityField;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

///
public final class UserAgentAggregationEntityMapper implements Codec<UserAgentAggregationEntity> {

    ///
    private static final String SOURCE_DECODE = "UserAgentAggregationEntityMapper.decode";

    ///
    @Override
    public Class<UserAgentAggregationEntity> getEncoderClass() {

        return UserAgentAggregationEntity.class;
    }

    ///..
    @Override
    public void encode(final BsonWriter writer, final UserAgentAggregationEntity value, final EncoderContext encoderContext) throws UnsupportedOperationException {

        throw new UnsupportedOperationException("Aggregations are read-only", new CauseContainer("UserAgentAggregationEntityMapper.encode"));
    }

    ///..
    @Override
    public UserAgentAggregationEntity decode(final BsonReader reader, final DecoderContext decoderContext) throws CodecException {

        try {

            String userAgent = null;
            long firstInvocation = 0;
            long lastInvocation = 0;
            long count = 0;

            reader.readStartDocument();

            while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

                final String name = reader.readName();

                switch(name) {

                    case EntityField.ID:

                        reader.readStartDocument();
                        userAgent = reader.readString(EntityField.USER_AGENT);
                        reader.readEndDocument();

                    break;

                    case EntityField.FIRST_INVOCATION: firstInvocation = reader.readInt64(); break;
                    case EntityField.LAST_INVOCATION: lastInvocation = reader.readInt64(); break;
                    case EntityField.COUNT: count = reader.readInt32(); break;

                    default: throw new CodecException("Unknown field '" + name + "'", SOURCE_DECODE);
                }
            }

            reader.readEndDocument();
            return new UserAgentAggregationEntity(userAgent, firstInvocation, lastInvocation, count);
        }

        catch(final IllegalStateException exc) {

            throw new CodecException(GenericUtils.WRAPPED_EXCEPTION_MSG, SOURCE_DECODE, exc);
        }
    }

    ///
}
