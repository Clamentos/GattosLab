package io.github.clamentos.gattoslab.observability.metrics.entities.aggregations;

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

        "Platform threads",

        "Classes loaded",

        "Sockets active",
        "Sockets opened",
        "Sockets closed",

        "File reads",
        "File writes",
        "Socket reads",
        "Socket writes",

        "GC counts",
        "GC pauses (ms)",

        "CPU load JVM user",
        "CPU load JVM system",
        "CPU load machine total",

        "System memory used",
        "Meta space used",
        "Heap used",

        "Storage used",

        "Equilibrium"
    };

    ///..
    private static final String[] chartKeys = new String[]{

        "threads",
        "classes",
        "ioResources",
        "ioUtilization",
        "garbageCollector",
        "cpuUtilization",
        "memoryUtilization",
        "storageUtilization",
        "requestMetricsEquilibrium"
    };

    ///.
    private long platformThreads;               // spot               (+-)  -> avg [count]
    private long classesLoaded;                 // spot               (+-)  -> avg [count]
    private long sockets;                       // spot               (+-)  -> avg [count]
    private long socketsOpened;                 // spot               (+)   -> max [count]
    private long socketsClosed;                 // spot               (+)   -> max [count]
    private long fileReads;                     // accumulate & reset (+-)  -> sum [count]
    private long fileWrites;                    // accumulate & reset (+-)  -> sum [count]
    private long socketReads;                   // accumulate & reset (+-)  -> sum [count]
    private long socketWrites;                  // accumulate & reset (+-)  -> sum [count]
    private long gcCounts;                      // accumulate & reset (+-)  -> sum [count]
    private long gcPauseMilliseconds;           // accumulate & reset (+-)  -> sum [duration]
    private long cpuLoadJvmUser;                // accumulate & reset (+-)  -> avg [%]
    private long cpuLoadJvmSystem;              // accumulate & reset (+-)  -> avg [%]
    private long cpuLoadMachineTotal;           // accumulate & reset (+-)  -> avg [%]
    private long systemMemoryUsed;              // accumulate & reset (+-)  -> avg [bytes]
    private long metaSpaceUsed;                 // spot               (+-)  -> avg [bytes]
    private long heapUsed;                      // spot               (+-)  -> avg [bytes]
    private long storageUsed;                   // spot               (+-)  -> avg [bytes]
    private long requestMetricsEquilibrium;     // spot               (+-)  -> sum [pressure]

    ///..
    private long count;

    ///.
    public static String[] getProbeNames() {

        return probeNames;
    }

    ///..
    public static String[] getChartKeys() {

        return chartKeys;
    }

    ///
}
