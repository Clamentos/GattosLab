package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.http.server.StreamWriter;
import io.github.clamentos.gattoslab.observability.Entity;

///..
import java.io.IOException;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class SystemMetricsEntity implements Entity {

    ///
    private final long id;
    private final long timestamp;
    private final long platformThreads;
    private final long classesLoaded;
    private final long fileReads;
    private final long fileWrites;
    private final long socketReads;
    private final long socketWrites;
    private final long gcCounts;
    private final long gcPause;
    private final long cpuLoadJvmUser;
    private final long cpuLoadJvmSystem;
    private final long cpuLoadMachineTotal;
    private final long systemMemoryUsed;
    private final long metaSpaceUsed;
    private final long heapUsed;
    private final long storageUsed;
    private final long requestMetricsEquilibrium;

    ///
    @Override
    public void stream(final StreamWriter writer) throws IOException {

        writer.write(Long.toString(this.id));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.timestamp));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.platformThreads));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.classesLoaded));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.fileReads));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.fileWrites));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.socketReads));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.socketWrites));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.gcCounts));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.gcPause));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.cpuLoadJvmUser));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.cpuLoadJvmSystem));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.cpuLoadMachineTotal));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.systemMemoryUsed));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.metaSpaceUsed));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.heapUsed));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.storageUsed));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.requestMetricsEquilibrium));
    }

    ///
}
