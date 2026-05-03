package io.github.clamentos.gattoslab.observability.logging.mappers;

///
import io.github.clamentos.gattoslab.exceptions.CodecException;
import io.github.clamentos.gattoslab.observability.logging.entities.LogEntity;
import io.github.clamentos.gattoslab.observability.logging.entities.LogEntityExceptionEntry;
import io.github.clamentos.gattoslab.persistence.EntityField;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
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
    private static final String SOURCE_DECODE = "LogEntitymapper.decode";

    ///
    @Override
    public Class<LogEntity> getEncoderClass() {

        return LogEntity.class;
    }

    ///..
    @Override
    public void encode(final BsonWriter writer, final LogEntity entity, final EncoderContext encoderContext) {

        writer.writeStartDocument();

        writer.writeObjectId(EntityField.ID, entity.getId());
        writer.writeInt64(EntityField.TIMESTAMP, entity.getTimestamp());
        writer.writeString(EntityField.SEVERITY, entity.getSeverity());
        writer.writeString(EntityField.THREAD, entity.getThread());
        writer.writeString(EntityField.LOGGER, entity.getLogger());

        if(entity.getMessage() != null) writer.writeString(EntityField.MESSAGE, entity.getMessage());

        if(entity.getException() != null) {

            final LogEntityExceptionEntry exception = entity.getException();

            writer.writeName(EntityField.EXCEPTION);
            writer.writeStartDocument();

            writer.writeString(EntityField.CLASS_NAME, exception.getClassName());
            if(exception.getMessage() != null) writer.writeString(EntityField.MESSAGE, exception.getMessage());

            if(exception.getStacktrace() != null) {

                writer.writeStartArray(EntityField.STACKTRACE);
                for(final String trace : exception.getStacktrace()) writer.writeString(trace);
                writer.writeEndArray();
            }

            writer.writeEndDocument();
        }

        writer.writeEndDocument();
    }

    ///..
    @Override
    public LogEntity decode(final BsonReader reader, final DecoderContext decoderContext) throws CodecException {

        try {

            ObjectId id = null;
            long timestamp = 0;
            String severity = null;
            String thread = null;
            String logger = null;
            String message = null;
            LogEntityExceptionEntry exception = null;

            reader.readStartDocument();

            while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

                final String name = reader.readName();

                switch(name) {

                    case EntityField.ID: id = reader.readObjectId(); break;
                    case EntityField.TIMESTAMP: timestamp = reader.readInt64(); break;
                    case EntityField.SEVERITY: severity = reader.readString(); break;
                    case EntityField.THREAD: thread = reader.readString(); break;
                    case EntityField.LOGGER: logger = reader.readString(); break;
                    case EntityField.MESSAGE: message = reader.readString(); break;

                    case EntityField.EXCEPTION:

                        String className = null;
                        String excMessage = null;
                        List<String> stacktrace = null;

                        reader.readStartDocument();

                        while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

                            final String innerName = reader.readName();

                            switch(innerName) {

                                case EntityField.CLASS_NAME: className = reader.readString(); break;
                                case EntityField.MESSAGE: excMessage = reader.readString(); break;
                                case EntityField.STACKTRACE: stacktrace = GenericUtils.readList(reader, String.class); break;

                                default: throw new CodecException("Unknown field '" + name + "'", SOURCE_DECODE);
                            }
                        }

                        reader.readEndDocument();
                        exception = new LogEntityExceptionEntry(className, excMessage, stacktrace);

                    break;

                    default: throw new CodecException("Unknown field '" + name + "'", SOURCE_DECODE);
                }
            }

            reader.readEndDocument();
            return new LogEntity(id, timestamp, severity, thread, logger, message, exception);
        }

        catch(final IllegalStateException exc) {

            throw new CodecException(GenericUtils.WRAPPED_EXCEPTION_MSG, SOURCE_DECODE, exc);
        }
    }

    ///
}
