package io.github.clamentos.gattoslab.observability.metrics.mappers;

///
import io.github.clamentos.gattoslab.observability.metrics.entities.SystemMetricsEntity;
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
public final class SystemMetricsEntityMapper implements Codec<SystemMetricsEntity> {

    ///
    @Override
    public Class<SystemMetricsEntity> getEncoderClass() {

        return SystemMetricsEntity.class;
    }

    ///..
    @Override
    public void encode(final BsonWriter writer, final SystemMetricsEntity entity, final EncoderContext encoderContext) {

        writer.writeStartDocument();

        writer.writeObjectId(EntityField.ID, entity.getId());
        writer.writeInt64(EntityField.TIMESTAMP, entity.getTimestamp());
        writer.writeInt64(EntityField.VIRTUAL_THREADS, entity.getVirtualThreads());
        writer.writeInt64(EntityField.PLATFORM_THREADS, entity.getPlatformThreads());
        writer.writeInt64(EntityField.CLASSES_LOADED, entity.getClassesLoaded());
        writer.writeInt64(EntityField.FILE_READS, entity.getFileReads());
        writer.writeInt64(EntityField.FILE_WRITES, entity.getFileWrites());
        writer.writeInt64(EntityField.SOCKET_READS, entity.getSocketReads());
        writer.writeInt64(EntityField.SOCKET_WRITES, entity.getSocketWrites());
        writer.writeInt64(EntityField.GC_COUNTS, entity.getGcCounts());
        writer.writeInt64(EntityField.GC_PAUSE, entity.getGcPause());
        writer.writeInt64(EntityField.CPU_LOAD_JVM_USER, entity.getCpuLoadJvmUser());
        writer.writeInt64(EntityField.CPU_LOAD_JVM_SYSTEM, entity.getCpuLoadJvmSystem());
        writer.writeInt64(EntityField.CPU_LOAD_MACHINE_TOTAL, entity.getCpuLoadMachineTotal());
        writer.writeInt64(EntityField.SYSTEM_MEMORY_USED, entity.getSystemMemoryUsed());
        writer.writeInt64(EntityField.META_SPACE_USED, entity.getMetaSpaceUsed());
        writer.writeInt64(EntityField.DIRECT_BUFFERS_USED, entity.getDirectBuffersUsed());
        writer.writeInt64(EntityField.DIRECT_BUFFERS_MEMORY_USED, entity.getDirectBuffersMemoryUsed());
        writer.writeInt64(EntityField.HEAP_USED, entity.getHeapUsed());
        writer.writeInt64(EntityField.STORAGE_USED, entity.getStorageUsed());

        writer.writeEndDocument();
    }

    ///..
    @Override
    public SystemMetricsEntity decode(final BsonReader reader, final DecoderContext decoderContext) {

        ObjectId id = null;
        long timestamp = 0;
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

                case EntityField.ID: id = reader.readObjectId(); break;
                case EntityField.TIMESTAMP: timestamp = reader.readInt64(); break;
                case EntityField.VIRTUAL_THREADS: virtualThreads = reader.readInt64(); break;
                case EntityField.PLATFORM_THREADS: platformThreads = reader.readInt64(); break;
                case EntityField.CLASSES_LOADED: classesLoaded = reader.readInt64(); break;
                case EntityField.FILE_READS: fileReads = reader.readInt64(); break;
                case EntityField.FILE_WRITES: fileWrites = reader.readInt64(); break;
                case EntityField.SOCKET_READS: socketReads = reader.readInt64(); break;
                case EntityField.SOCKET_WRITES: socketWrites = reader.readInt64(); break;
                case EntityField.GC_COUNTS: gcCounts = reader.readInt64(); break;
                case EntityField.GC_PAUSE: gcPause = reader.readInt64(); break;
                case EntityField.CPU_LOAD_JVM_USER: cpuLoadJvmUser = reader.readInt64(); break;
                case EntityField.CPU_LOAD_JVM_SYSTEM: cpuLoadJvmSystem = reader.readInt64(); break;
                case EntityField.CPU_LOAD_MACHINE_TOTAL: cpuLoadMachineTotal = reader.readInt64(); break;
                case EntityField.SYSTEM_MEMORY_USED: systemMemoryUsed = reader.readInt64(); break;
                case EntityField.META_SPACE_USED: metaSpaceUsed = reader.readInt64(); break;
                case EntityField.DIRECT_BUFFERS_USED: directBuffersUsed = reader.readInt64(); break;
                case EntityField.DIRECT_BUFFERS_MEMORY_USED: directBuffersMemoryUsed = reader.readInt64(); break;
                case EntityField.HEAP_USED: heapUsed = reader.readInt64(); break;
                case EntityField.STORAGE_USED: storageUsed = reader.readInt64(); break;

                default: throw new IllegalArgumentException("SystemMetricsEntityMapper.decode~Unknown field name " + name);
            }
        }

        reader.readEndDocument();

        return new SystemMetricsEntity(

            id,
            timestamp,
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

    ///
}
