package io.github.clamentos.gattoslab.observability.metrics.mappers;

///
import io.github.clamentos.gattoslab.exceptions.CauseContainer;
import io.github.clamentos.gattoslab.exceptions.CodecException;
import io.github.clamentos.gattoslab.observability.metrics.entities.RequestMetricsAggregateEntity;
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
public final class RequestMetricsAggregateEntityMapper implements Codec<RequestMetricsAggregateEntity> {

    ///
    private static final String SOURCE_DECODE = "RequestMetricsAggregateEntityMapper.decode";

    ///
    @Override
    public Class<RequestMetricsAggregateEntity> getEncoderClass() {

        return RequestMetricsAggregateEntity.class;
    }

    ///..
    @Override
    public void encode(final BsonWriter writer, final RequestMetricsAggregateEntity value, final EncoderContext encoderContext) throws UnsupportedOperationException {

        throw new UnsupportedOperationException("Aggregations are read-only", new CauseContainer("RequestMetricsAggregateEntityMapper.encode"));
    }

    ///..
    @Override
    public RequestMetricsAggregateEntity decode(final BsonReader reader, final DecoderContext decoderContext) throws CodecException {

        try {

            String key = null;
            long timeSlot = 0;
            boolean isOthers = false;
            int rate = 0;
            int[] latencyDistribution = null;

            reader.readStartDocument();

            while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

                final String name = reader.readName();

                switch(name) {

                    case EntityField.KEY: key = reader.readString(); break;
                    case EntityField.TIME_SLOT: timeSlot = (long)reader.readDouble(); break;
                    case EntityField.IS_OTHERS: isOthers = reader.readBoolean(); break;
                    case EntityField.RATE: rate = reader.readInt32(); break;

                    case EntityField.LATENCY_DISTRIBUTION:

                        latencyDistribution = new int[EntityField.LATENCIES.size()];

                        reader.readStartArray();
                        for(int i = 0; i < latencyDistribution.length; i++) latencyDistribution[i] = reader.readInt32();
                        reader.readEndArray();

                    break;

                    case EntityField.ID: reader.readObjectId(); break;
                    default: throw new CodecException("Unknown field '" + name + "'", SOURCE_DECODE);
                }
            }

            reader.readEndDocument();
            return new RequestMetricsAggregateEntity(key, timeSlot, isOthers, rate, latencyDistribution);
        }

        catch(final IllegalStateException exc) {

            throw new CodecException(GenericUtils.WRAPPED_EXCEPTION_MSG, SOURCE_DECODE, exc);
        }
    }

    ///
}
