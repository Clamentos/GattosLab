package io.github.clamentos.gattoslab.observability.metrics.mappers;

///
import io.github.clamentos.gattoslab.observability.metrics.entities.SystemMetricsEntity;

///..
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

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

        writer.writeObjectId("_id", entity.getId());
        writer.writeInt64("timestamp", entity.getTimestamp());
        writer.writeInt64("virtualThreads", entity.getVirtualThreads());
        writer.writeInt64("platformThreads", entity.getPlatformThreads());
        writer.writeInt64("classesLoaded", entity.getClassesLoaded());
        writer.writeInt64("fileReads", entity.getFileReads());
        writer.writeInt64("fileWrites", entity.getFileWrites());
        writer.writeInt64("socketReads", entity.getSocketReads());
        writer.writeInt64("socketWrites", entity.getSocketWrites());
        writer.writeInt64("gcCounts", entity.getGcCounts());
        writer.writeInt64("gcPause", entity.getGcPause());
        writer.writeInt64("cpuLoadJvmUser", entity.getCpuLoadJvmUser());
        writer.writeInt64("cpuLoadJvmSystem", entity.getCpuLoadJvmSystem());
        writer.writeInt64("cpuLoadMachineTotal", entity.getCpuLoadMachineTotal());
        writer.writeInt64("systemMemoryUsed", entity.getSystemMemoryUsed());
        writer.writeInt64("metaSpaceUsed", entity.getMetaSpaceUsed());
        writer.writeInt64("directBuffersUsed", entity.getDirectBuffersUsed());
        writer.writeInt64("directBuffersMemoryUsed", entity.getDirectBuffersMemoryUsed());
        writer.writeInt64("heapUsed", entity.getHeapUsed());
        writer.writeInt64("storageUsed", entity.getStorageUsed());

        writer.writeEndDocument();
    }

    ///..
    @Override
    public SystemMetricsEntity decode(final BsonReader reader, final DecoderContext decoderContext) {

        reader.readStartDocument();

        final SystemMetricsEntity entity = new SystemMetricsEntity(

            reader.readObjectId(),
            reader.readInt64("timestamp"),
            reader.readInt64("virtualThreads"),
            reader.readInt64("platformThreads"),
            reader.readInt64("classesLoaded"),
            reader.readInt64("fileReads"),
            reader.readInt64("fileWrites"),
            reader.readInt64("socketReads"),
            reader.readInt64("socketWrites"),
            reader.readInt64("gcCounts"),
            reader.readInt64("gcPause"),
            reader.readInt64("cpuLoadJvmUser"),
            reader.readInt64("cpuLoadJvmSystem"),
            reader.readInt64("cpuLoadMachineTotal"),
            reader.readInt64("systemMemoryUsed"),
            reader.readInt64("metaSpaceUsed"),
            reader.readInt64("directBuffersUsed"),
            reader.readInt64("directBuffersMemoryUsed"),
            reader.readInt64("heapUsed"),
            reader.readInt64("storageUsed")
        );

        reader.readEndDocument();
        return entity;
    }

    ///
}
