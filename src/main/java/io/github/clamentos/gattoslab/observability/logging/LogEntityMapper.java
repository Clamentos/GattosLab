package io.github.clamentos.gattoslab.observability.logging;

///
import java.util.ArrayList;
import java.util.List;

///..
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.ObjectId;

///
public final class LogEntityMapper implements Codec<LogEntity> {

    ///
    @Override
    public Class<LogEntity> getEncoderClass() {

        return LogEntity.class;
    }

    ///..
    @Override
    public void encode(final BsonWriter writer, final LogEntity entity, final EncoderContext encoderContext) {

        writer.writeStartDocument();

        writer.writeObjectId("_id", entity.getId());
        writer.writeInt64("timestamp", entity.getTimestamp());
        writer.writeString("severity", entity.getSeverity());
        writer.writeString("thread", entity.getThread());
        writer.writeString("logger", entity.getLogger());
        writer.writeString("message", entity.getMessage());

        if(entity.getException() != null) {

            final LogEntityStackTrace exception = entity.getException();

            writer.writeStartDocument();

            writer.writeString("className", exception.getClassName());
            writer.writeString("message", exception.getMessage());

            if(exception.getStacktrace() != null) {

                writer.writeStartArray("stacktrace");
                for(final String trace : exception.getStacktrace()) writer.writeString(trace);
                writer.writeEndArray();
            }

            writer.writeEndDocument();
        }

        writer.writeEndDocument();
    }

    ///..
    @Override
    public LogEntity decode(final BsonReader reader, final DecoderContext decoderContext) {

        reader.readStartDocument();

        final ObjectId id = reader.readObjectId("_id");
        final long timestamp = reader.readInt64("timestamp");
        final String severity = reader.readString("severity");
        final String thread = reader.readString("thread");
        final String logger = reader.readString("logger");
        final String message = reader.readString("message");

        LogEntityStackTrace exception = null;

        if(reader.getCurrentBsonType() == BsonType.DOCUMENT) {

            reader.readStartDocument();

            final String className = reader.readString("className");
            final String exceptionMessage = reader.readString("message");

            reader.readEndDocument();
            List<String> stacktrace = null;

            if(reader.getCurrentBsonType() == BsonType.ARRAY) {

                stacktrace = new ArrayList<>();

                reader.readStartArray();
                while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) stacktrace.add(reader.readString());
                reader.readEndArray();
            }

            else {

                reader.readNull();
            }

            exception = new LogEntityStackTrace(className, exceptionMessage, stacktrace);
        }

        else {

            reader.readNull();
        }

        return new LogEntity(id, timestamp, severity, thread, logger, message, exception);
    }

    ///
}
