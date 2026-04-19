package io.github.clamentos.gattoslab.observability.metrics.mappers;

///
import io.github.clamentos.gattoslab.observability.metrics.entities.UserAgentAggregationEntity;
import io.github.clamentos.gattoslab.persistence.EntityField;

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
    @Override
    public Class<UserAgentAggregationEntity> getEncoderClass() {

        return UserAgentAggregationEntity.class;
    }

    ///..
    @Override
    public void encode(final BsonWriter writer, final UserAgentAggregationEntity value, final EncoderContext encoderContext) {

        throw new UnsupportedOperationException("Aggregations are read-only");
    }

    ///..
    @Override
    public UserAgentAggregationEntity decode(final BsonReader reader, final DecoderContext decoderContext) {

        String userAgent = null;
        long firstInvocation = 0;
        long lastInvocation = 0;
        long count = 0;

        reader.readStartDocument();

        while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

            final String name = reader.readName();

            switch(name) {

                case EntityField.USER_AGENT: userAgent = reader.readString(); break;
                case EntityField.FIRST_INVOCATION: firstInvocation = reader.readInt64(); break;
                case EntityField.LAST_INVOCATION: lastInvocation = reader.readInt64(); break;
                case EntityField.COUNT: count = reader.readInt64(); break;

                case EntityField.ID: break;
                default: throw new IllegalArgumentException("Unknown field name " + name);
            }
        }

        reader.readEndDocument();
        return new UserAgentAggregationEntity(userAgent, firstInvocation, lastInvocation, count);
    }

    ///
}
