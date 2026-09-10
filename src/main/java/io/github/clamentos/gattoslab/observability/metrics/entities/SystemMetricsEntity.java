package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.FastAsciiJoiner;
import io.github.clamentos.gattoslab.observability.Printable;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class SystemMetricsEntity implements Printable {

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
    private final long directBuffersUsed;
    private final long directBuffersMemoryUsed;
    private final long heapUsed;
    private final long storageUsed;
    private final long requestMetricsEquilibrium;

    ///
    @Override
    public void appendBytes(final FastAsciiJoiner joiner) {

        joiner.add(Long.toString(this.id));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.timestamp));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.platformThreads));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.classesLoaded));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.fileReads));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.fileWrites));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.socketReads));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.socketWrites));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.gcCounts));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.gcPause));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.cpuLoadJvmUser));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.cpuLoadJvmSystem));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.cpuLoadMachineTotal));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.systemMemoryUsed));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.metaSpaceUsed));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.directBuffersUsed));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.directBuffersMemoryUsed));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.heapUsed));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.storageUsed));
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.requestMetricsEquilibrium));
    }

    ///
}
