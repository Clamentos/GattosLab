package io.github.clamentos.gattoslab.observability.metrics.mappers;

///
import io.github.clamentos.gattoslab.observability.metrics.entities.PathInvocationAggregationEntity;
import io.github.clamentos.gattoslab.persistence.EntityField;

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

        String path = null;
        long firstInvocation = 0;
        long lastInvocation = 0;
        long count = 0;
        Set<Short> httpStatuses = null;

        reader.readStartDocument();

        while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

            final String name = reader.readName();

            switch(name) {

                case EntityField.PATH: path = reader.readString(); break;
                case EntityField.FIRST_INVOCATION: firstInvocation = reader.readInt64(); break;
                case EntityField.LAST_INVOCATION: lastInvocation = reader.readInt64(); break;
                case EntityField.COUNT: count = reader.readInt64(); break;

                case EntityField.HTTP_STATUSES:

                    reader.readStartArray();
                    httpStatuses = new HashSet<>();
                    while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) httpStatuses.add((short)reader.readInt64());
                    reader.readEndArray();

                break;

                case EntityField.ID: break;
                default: throw new IllegalArgumentException("Unknown field name " + name);
            }
        }

        reader.readEndDocument();
        short[] httpStatusesArray = null;

        if(httpStatuses != null) {

            int i = 0;
            httpStatusesArray = new short[httpStatuses.size()];
            for(final Short status : httpStatuses) httpStatusesArray[i++] = status;
        }

        return new PathInvocationAggregationEntity(path, firstInvocation, lastInvocation, count, httpStatusesArray);
    }

    ///
}
