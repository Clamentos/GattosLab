package io.github.clamentos.gattoslab.observability.metrics.mappers;

///
import io.github.clamentos.gattoslab.observability.metrics.entities.PathInvocationAggregationEntity;

///..
import java.util.HashSet;
import java.util.Set;

///..
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

///
public final class PathInvocationAggregationEntityMapper implements Codec<PathInvocationAggregationEntity> {

    ///
    @Override
    public Class<PathInvocationAggregationEntity> getEncoderClass() {

        return PathInvocationAggregationEntity.class;
    }

    ///..
    @Override
    public void encode(final BsonWriter writer, final PathInvocationAggregationEntity value, final EncoderContext encoderContext) {

        throw new UnsupportedOperationException("Aggregations are read-only");
    }

    ///..
    @Override
    public PathInvocationAggregationEntity decode(final BsonReader reader, final DecoderContext decoderContext) {

        reader.readStartDocument();

        reader.readName("httpStatuses");
        reader.readStartArray();

        final Set<Short> httpStatuses = new HashSet<>();
        while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) httpStatuses.add((short)reader.readInt32());

        reader.readEndArray();

        final PathInvocationAggregationEntity entity = new PathInvocationAggregationEntity(

            reader.readString("path"),
            httpStatuses,
            reader.readInt64("firstInvocation"),
            reader.readInt64("lastInvocation"),
            reader.readInt64("count")
        );

        reader.readEndDocument();
        return entity;
    }

    ///
}
