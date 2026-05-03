package io.github.clamentos.gattoslab.observability.metrics.mappers;

///
import io.github.clamentos.gattoslab.exceptions.CauseContainer;
import io.github.clamentos.gattoslab.exceptions.CodecException;
import io.github.clamentos.gattoslab.observability.metrics.entities.PathInvocationAggregationEntity;
import io.github.clamentos.gattoslab.persistence.EntityField;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
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
    private static final String SOURCE_DECODE = "PathInvocationAggregationEntityMapper.decode";

    ///
    @Override
    public Class<PathInvocationAggregationEntity> getEncoderClass() {

        return PathInvocationAggregationEntity.class;
    }

    ///..
    @Override
    public void encode(final BsonWriter writer, final PathInvocationAggregationEntity value, final EncoderContext encoderContext) throws UnsupportedOperationException {

        throw new UnsupportedOperationException("Aggregations are read-only", new CauseContainer("PathInvocationAggregationEntityMapper.encode"));
    }

    ///..
    @Override
    public PathInvocationAggregationEntity decode(final BsonReader reader, final DecoderContext decoderContext) throws CodecException {

        try {

            String path = null;
            long firstInvocation = 0;
            long lastInvocation = 0;
            long count = 0;
            boolean isOther = false;
            Set<Short> httpStatuses = null;

            reader.readStartDocument();

            while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

                final String name = reader.readName();

                switch(name) {

                    case EntityField.ID:

                        reader.readStartDocument();
                        path = reader.readString(EntityField.PATH);
                        reader.readEndDocument();

                    break;

                    case EntityField.FIRST_INVOCATION: firstInvocation = reader.readInt64(); break;
                    case EntityField.LAST_INVOCATION: lastInvocation = reader.readInt64(); break;
                    case EntityField.COUNT: count = reader.readInt32(); break;
                    case EntityField.IS_OTHERS: isOther = reader.readBoolean(); break;
                    case EntityField.HTTP_STATUSES: httpStatuses = GenericUtils.readSet(reader, Short.class); break;

                    default: throw new CodecException("Unknown field '" + name + "'", SOURCE_DECODE);
                }
            }

            reader.readEndDocument();
            short[] httpStatusesArray = null;

            if(httpStatuses != null) {

                int i = 0;
                httpStatusesArray = new short[httpStatuses.size()];
                for(final Short status : httpStatuses) httpStatusesArray[i++] = status;
            }

            return new PathInvocationAggregationEntity(path, firstInvocation, lastInvocation, count, isOther, httpStatusesArray);
        }

        catch(final IllegalStateException exc) {

            throw new CodecException(GenericUtils.WRAPPED_EXCEPTION_MSG, SOURCE_DECODE, exc);
        }
    }

    ///
}
