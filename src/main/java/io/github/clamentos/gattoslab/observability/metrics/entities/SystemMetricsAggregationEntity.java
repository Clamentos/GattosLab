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
    private static final String[] probes = new String[]{

        "platformThreads",
        "classesLoaded",
        "fileReads",
        "fileWrites",
        "socketReads",
        "socketWrites",
        "gcCounts",
        "gcPause",
        "cpuLoadJvmUser",
        "cpuLoadJvmSystem",
        "cpuLoadMachineTotal",
        "systemMemoryUsed",
        "metaSpaceUsed",
        "directBuffersUsed",
        "directBuffersMemoryUsed",
        "heapUsed",
        "storageUsed",
        "requestMetricsEquilibrium"
    };

    ///.
    private long platformThreads;
    private long classesLoaded;
    private long fileReads;
    private long fileWrites;
    private long socketReads;
    private long socketWrites;
    private long gcCounts;
    private long gcPause;
    private long cpuLoadJvmUser;
    private long cpuLoadJvmSystem;
    private long cpuLoadMachineTotal;
    private long systemMemoryUsed;
    private long metaSpaceUsed;
    private long directBuffersUsed;
    private long directBuffersMemoryUsed;
    private long heapUsed;
    private long storageUsed;
    private long requestMetricsEquilibrium;

    ///..
    private long count;

    ///.
    public static String[] getProbeNames() {

        return probes;
    }

    ///
}
