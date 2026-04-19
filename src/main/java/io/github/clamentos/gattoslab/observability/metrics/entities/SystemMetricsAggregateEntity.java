package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class SystemMetricsAggregateEntity {

    ///
    private final long timeSlot;
    private final long virtualThreads;
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

    ///
}
