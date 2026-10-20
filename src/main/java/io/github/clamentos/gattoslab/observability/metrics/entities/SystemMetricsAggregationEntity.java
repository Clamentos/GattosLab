package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import lombok.Getter;
import lombok.Setter;

///
@Getter
@Setter

///
public final class SystemMetricsAggregationEntity {

    ///
    private static final String[] probeNames = new String[]{

        "platformThreads",
        "classesLoaded",
        "fileReads",
        "fileWrites",
        "sockets",
        "socketReads",
        "socketWrites",
        "gcCounts",
        "gcPauseMilliseconds",
        "cpuLoadJvmUser",
        "cpuLoadJvmSystem",
        "cpuLoadMachineTotal",
        "systemMemoryUsed",
        "metaSpaceUsed",
        "heapUsed",
        "storageUsed",
        "requestMetricsEquilibrium"
    };

    ///
    private long platformThreads;
    private long classesLoaded;
    private long fileReads;
    private long fileWrites;
    private long sockets;
    private long socketReads;
    private long socketWrites;
    private long gcCounts;
    private long gcPauseMilliseconds;
    private long cpuLoadJvmUser;
    private long cpuLoadJvmSystem;
    private long cpuLoadMachineTotal;
    private long systemMemoryUsed;
    private long metaSpaceUsed;
    private long heapUsed;
    private long storageUsed;
    private long requestMetricsEquilibrium;

    ///..
    private long count;

    ///
    public static String[] getProbeNames() {

        return probeNames;
    }

    ///
}
