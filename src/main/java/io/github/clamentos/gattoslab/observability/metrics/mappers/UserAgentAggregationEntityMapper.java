package io.github.clamentos.gattoslab.observability.metrics.mappers;

///
import io.github.clamentos.gattoslab.observability.metrics.entities.UserAgentAggregationEntity;

///..
import org.bson.BsonReader;
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

        reader.readStartDocument();

        final UserAgentAggregationEntity entity = new UserAgentAggregationEntity(

            reader.readString("userAgent"),
            reader.readInt64("firstInvocation"),
            reader.readInt64("lastInvocation"),
            reader.readInt64("count")
        );

        reader.readEndDocument();
        return entity;
    }

    ///
}
