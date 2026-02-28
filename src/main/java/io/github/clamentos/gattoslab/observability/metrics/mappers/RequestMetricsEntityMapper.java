package io.github.clamentos.gattoslab.observability.metrics.mappers;

///
import io.github.clamentos.gattoslab.observability.metrics.entities.RequestMetricsEntity;

///..
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

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

        writer.writeObjectId("_id", entity.getId());
        writer.writeInt64("timestamp", entity.getTimestamp());
        writer.writeInt32("latency", entity.getLatency());
        writer.writeString("path", entity.getPath());
        writer.writeString("userAgent", entity.getUserAgent());
        writer.writeBoolean("isOthers", entity.isOthers());
        writer.writeInt32("httpStatus", entity.getHttpStatus());

        writer.writeEndDocument();
    }

    ///..
    @Override
    public RequestMetricsEntity decode(final BsonReader reader, final DecoderContext decoderContext) {

        reader.readStartDocument();

        final RequestMetricsEntity entity = new RequestMetricsEntity(

            reader.readObjectId("_id"),
            reader.readInt64("timestamp"),
            reader.readInt32("latency"),
            reader.readString("path"),
            reader.readString("userAgent"),
            reader.readBoolean("isOthers"),
            reader.readInt32("httpStatus")
        );

        reader.readEndDocument();
        return entity;
    }

    ///
}
