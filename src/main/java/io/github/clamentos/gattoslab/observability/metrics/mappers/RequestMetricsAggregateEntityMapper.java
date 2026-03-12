package io.github.clamentos.gattoslab.observability.metrics.mappers;

///
import io.github.clamentos.gattoslab.observability.metrics.entities.RequestMetricsAggregateEntity;

///..
import java.util.ArrayList;
import java.util.List;

///..
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

///
public final class RequestMetricsAggregateEntityMapper implements Codec<RequestMetricsAggregateEntity> {

    ///
    @Override
    public Class<RequestMetricsAggregateEntity> getEncoderClass() {

        return RequestMetricsAggregateEntity.class;
    }

    ///..
    @Override
    public void encode(final BsonWriter writer, final RequestMetricsAggregateEntity value, final EncoderContext encoderContext) {

        throw new UnsupportedOperationException("Aggregations are read-only");
    }

    ///..
    @Override
    public RequestMetricsAggregateEntity decode(final BsonReader reader, final DecoderContext decoderContext) {

        reader.readStartDocument();

        reader.readName("latencyDistribution");
        reader.readStartArray();

        final List<Long> latencyDistribution = new ArrayList<>();
        while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) latencyDistribution.add(reader.readInt64());

        reader.readEndArray();

        final RequestMetricsAggregateEntity entity = new RequestMetricsAggregateEntity(

            reader.readString("key"),
            reader.readInt64("timeSlot"),
            reader.readBoolean("isOthers"),
            reader.readInt32("rate"),
            latencyDistribution
        );

        reader.readEndDocument();
        return entity;
    }

    ///
}
