package io.github.clamentos.gattoslab.observability.metrics.mappers;

///
import io.github.clamentos.gattoslab.observability.metrics.entities.RequestMetricsEntity;
import io.github.clamentos.gattoslab.persistence.EntityField;

///..
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.ObjectId;

///
public class RequestMetricsEntityMapper implements Codec<RequestMetricsEntity> {

    ///
    @Override
    public Class<RequestMetricsEntity> getEncoderClass() {

        return RequestMetricsEntity.class;
    }

    ///..
    @Override
    public void encode(final BsonWriter writer, final RequestMetricsEntity entity, final EncoderContext encoderContext) {

        writer.writeStartDocument();

        writer.writeObjectId(EntityField.ID, entity.getId());
        writer.writeInt64(EntityField.TIMESTAMP, entity.getTimestamp());
        writer.writeInt32(EntityField.LATENCY, entity.getLatency());
        writer.writeString(EntityField.PATH, entity.getPath());
        writer.writeString(EntityField.USER_AGENT, entity.getUserAgent());
        writer.writeBoolean(EntityField.IS_OTHERS, entity.isOthers());
        writer.writeInt32(EntityField.HTTP_STATUS, entity.getHttpStatus());

        writer.writeEndDocument();
    }

    ///..
    @Override
    public RequestMetricsEntity decode(final BsonReader reader, final DecoderContext decoderContext) {

        ObjectId id = null;
        long timestamp = 0;
        int latency = 0;
        String path = null;
        String userAgent = null;
        boolean isOthers = false;
        int httpStatus = 0;

        reader.readStartDocument();

        while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

            final String name = reader.readName();

            switch(name) {

                case EntityField.ID: id = reader.readObjectId(); break;
                case EntityField.TIMESTAMP: timestamp = reader.readInt64(); break;
                case EntityField.LATENCY: latency = reader.readInt32(); break;
                case EntityField.PATH: path = reader.readString(); break;
                case EntityField.USER_AGENT: userAgent = reader.readString(); break;
                case EntityField.IS_OTHERS: isOthers = reader.readBoolean(); break;
                case EntityField.HTTP_STATUS: httpStatus = reader.readInt32(); break;

                default: throw new IllegalArgumentException("Unknown field name " + name);
            }
        }

        reader.readEndDocument();
        return new RequestMetricsEntity(id, timestamp, latency, path, userAgent, isOthers, httpStatus);
    }

    ///
}
