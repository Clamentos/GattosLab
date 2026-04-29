package io.github.clamentos.gattoslab.observability.metrics.mappers;

///
import io.github.clamentos.gattoslab.observability.metrics.entities.RequestMetricsAggregateEntity;
import io.github.clamentos.gattoslab.persistence.EntityField;

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

        throw new UnsupportedOperationException("RequestMetricsAggregateEntityMapper.encode~Aggregations are read-only");
    }

    ///..
    @Override
    public RequestMetricsAggregateEntity decode(final BsonReader reader, final DecoderContext decoderContext) throws IllegalArgumentException {

        String key = null;
        long timeSlot = 0;
        boolean isOthers = false;
        int rate = 0;
        List<Integer> latencyDistribution = null;

        reader.readStartDocument();

        while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

            final String name = reader.readName();

            switch(name) {

                case EntityField.LATENCY_DISTRIBUTION:

                    reader.readStartArray();
                    latencyDistribution = new ArrayList<>();
                    while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) latencyDistribution.add(reader.readInt32());
                    reader.readEndArray();

                break;

                case EntityField.KEY: key = reader.readString(); break;
                case EntityField.TIME_SLOT: timeSlot = (long)reader.readDouble(); break;
                case EntityField.IS_OTHERS: isOthers = reader.readBoolean(); break;
                case EntityField.RATE: rate = reader.readInt32(); break;

                case EntityField.ID: reader.readObjectId(); break;
                default: throw new IllegalArgumentException("RequestMetricsAggregateEntityMapper.decode~Unknown field name " + name);
            }
        }

        reader.readEndDocument();
        int[] latencyDistributionArray = null;

        if(latencyDistribution != null) {

            int i = 0;
            latencyDistributionArray = new int[latencyDistribution.size()];
            for(final Integer latency : latencyDistribution) latencyDistributionArray[i++] = latency;
        }

        return new RequestMetricsAggregateEntity(key, timeSlot, isOthers, rate, latencyDistributionArray);
    }

    ///
}
