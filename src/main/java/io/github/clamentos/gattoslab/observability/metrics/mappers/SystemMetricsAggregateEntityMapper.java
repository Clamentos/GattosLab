package io.github.clamentos.gattoslab.observability.metrics.mappers;

///
import io.github.clamentos.gattoslab.observability.metrics.entities.SystemMetricsAggregateEntity;
import io.github.clamentos.gattoslab.persistence.EntityField;

///..
import org.bson.BsonReader;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

///
public final class SystemMetricsAggregateEntityMapper implements Codec<SystemMetricsAggregateEntity> {

    ///
    @Override
    public Class<SystemMetricsAggregateEntity> getEncoderClass() {

        return SystemMetricsAggregateEntity.class;
    }

    ///..
    @Override
    public void encode(final BsonWriter writer, final SystemMetricsAggregateEntity value, final EncoderContext encoderContext) {

        throw new UnsupportedOperationException("SystemMetricsAggregateEntityMapper.encode~Aggregations are read-only");
    }

    ///..
    @Override
    public SystemMetricsAggregateEntity decode(final BsonReader reader, final DecoderContext decoderContext) {

        long timeSlot = 0;
        long virtualThreads = 0;
        long platformThreads = 0;
        long classesLoaded = 0;
        long fileReads = 0;
        long fileWrites = 0;
        long socketReads = 0;
        long socketWrites = 0;
        long gcCounts = 0;
        long gcPause = 0;
        long cpuLoadJvmUser = 0;
        long cpuLoadJvmSystem = 0;
        long cpuLoadMachineTotal = 0;
        long systemMemoryUsed = 0;
        long metaSpaceUsed = 0;
        long directBuffersUsed = 0;
        long directBuffersMemoryUsed = 0;
        long heapUsed = 0;
        long storageUsed = 0;

        reader.readStartDocument();

        while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

            final String name = reader.readName();

            switch(name) {

                case EntityField.TIME_SLOT: timeSlot = this.readLong(reader); break;
                case EntityField.VIRTUAL_THREADS: virtualThreads = this.readLong(reader); break;
                case EntityField.PLATFORM_THREADS: platformThreads = this.readLong(reader); break;
                case EntityField.CLASSES_LOADED: classesLoaded = this.readLong(reader); break;
                case EntityField.FILE_READS: fileReads = this.readLong(reader); break;
                case EntityField.FILE_WRITES: fileWrites = this.readLong(reader); break;
                case EntityField.SOCKET_READS: socketReads = this.readLong(reader); break;
                case EntityField.SOCKET_WRITES: socketWrites = this.readLong(reader); break;
                case EntityField.GC_COUNTS: gcCounts = this.readLong(reader); break;
                case EntityField.GC_PAUSE: gcPause = this.readLong(reader); break;
                case EntityField.CPU_LOAD_JVM_USER: cpuLoadJvmUser = this.readLong(reader); break;
                case EntityField.CPU_LOAD_JVM_SYSTEM: cpuLoadJvmSystem = this.readLong(reader); break;
                case EntityField.CPU_LOAD_MACHINE_TOTAL: cpuLoadMachineTotal = this.readLong(reader); break;
                case EntityField.SYSTEM_MEMORY_USED: systemMemoryUsed = this.readLong(reader); break;
                case EntityField.META_SPACE_USED: metaSpaceUsed = this.readLong(reader); break;
                case EntityField.DIRECT_BUFFERS_USED: directBuffersUsed = this.readLong(reader); break;
                case EntityField.DIRECT_BUFFERS_MEMORY_USED: directBuffersMemoryUsed = this.readLong(reader); break;
                case EntityField.HEAP_USED: heapUsed = this.readLong(reader); break;
                case EntityField.STORAGE_USED: storageUsed = this.readLong(reader); break;

                case EntityField.ID: reader.readObjectId(); break;
                default: throw new IllegalArgumentException("SystemMetricsAggregateEntityMapper.decode~Unknown field name " + name);
            }
        }

        reader.readEndDocument();

        return new SystemMetricsAggregateEntity(

            timeSlot,
            virtualThreads,
            platformThreads,
            classesLoaded,
            fileReads,
            fileWrites,
            socketReads,
            socketWrites,
            gcCounts,
            gcPause,
            cpuLoadJvmUser,
            cpuLoadJvmSystem,
            cpuLoadMachineTotal,
            systemMemoryUsed,
            metaSpaceUsed,
            directBuffersUsed,
            directBuffersMemoryUsed,
            heapUsed,
            storageUsed
        );
    }

    ///.
    private long readLong(final BsonReader reader) {

        final BsonType type = reader.getCurrentBsonType();

        if(type.equals(BsonType.DOUBLE)) return (long) reader.readDouble();
        return reader.readInt64();
    }

    ///
}
