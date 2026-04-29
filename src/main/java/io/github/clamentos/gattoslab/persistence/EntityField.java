package io.github.clamentos.gattoslab.persistence;

///
import java.util.List;

///..
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

///
@NoArgsConstructor(access = AccessLevel.PRIVATE)

///
public class EntityField {

    ///
    // Used by all
    public static final String ID = "_id";

    ///..
    // Used by many
    public static final String TIMESTAMP = "timestamp";

    ///..
    // LogEntity
    public static final String SEVERITY = "severity";
    public static final String THREAD = "thread";
    public static final String LOGGER = "logger";
    public static final String MESSAGE = "message";
    public static final String CLASS_NAME = "className";
    public static final String STACKTRACE = "stacktrace";
    public static final String EXCEPTION = "exception";

    ///..
    // RequestMetricsEntity
    public static final String IS_OTHERS = "isOthers";
    public static final String PATH = "path";
    public static final String HTTP_STATUS = "httpStatus";
    public static final String USER_AGENT = "userAgent";
    public static final String LATENCY = "latency";

    ///..
    // SystemMetricsEntity
    public static final String VIRTUAL_THREADS = "virtualThreads";
    public static final String PLATFORM_THREADS = "platformThreads";
    public static final String CLASSES_LOADED = "classesLoaded";
    public static final String FILE_READS = "fileReads";
    public static final String FILE_WRITES = "fileWrites";
    public static final String SOCKET_READS = "socketReads";
    public static final String SOCKET_WRITES = "socketWrites";
    public static final String GC_COUNTS = "gcCounts";
    public static final String GC_PAUSE = "gcPause";
    public static final String CPU_LOAD_JVM_USER = "cpuLoadJvmUser";
    public static final String CPU_LOAD_JVM_SYSTEM = "cpuLoadJvmSystem";
    public static final String CPU_LOAD_MACHINE_TOTAL = "cpuLoadMachineTotal";
    public static final String SYSTEM_MEMORY_USED = "systemMemoryUsed";
    public static final String META_SPACE_USED = "metaSpaceUsed";
    public static final String DIRECT_BUFFERS_USED = "directBuffersUsed";
    public static final String DIRECT_BUFFERS_MEMORY_USED = "directBuffersMemoryUsed";
    public static final String HEAP_USED = "heapUsed";
    public static final String STORAGE_USED = "storageUsed";

    ///..
    // DynamicProperty
    public static final String KEY = "key";
    public static final String VALUE = "value";
    public static final String START = "start";
    public static final String END = "end";
    public static final String IPV4S = "ipv4s";
    public static final String IPV6S = "ipv6s";
    public static final String USER_AGENT_CONTAINS = "userAgentContains";

    ///..
    // Filters
    public static final String START_TIMESTAMP = "startTimestamp";
    public static final String END_TIMESTAMP = "endTimestamp";
    public static final String BUCKET_SIZE = "bucketSize";
    public static final String SEVERITIES = "severities";
    public static final String THREAD_PATTERN = "threadPattern";
    public static final String LOGGER_PATTERN = "loggerPattern";
    public static final String MESSAGE_PATTERN = "messagePattern";
    public static final String EXCEPTION_CLASS_PATTERN = "exceptionClassPattern";
    public static final String ONLY_OTHERS = "onlyOthers";
    public static final String PATH_PATTERN = "pathPattern";
    public static final String USER_AGENT_PATTERN = "userAgentPattern";

    ///..
    // Aggregation pipelines
    public static final String TIME_SLOT = "timeSlot";
    public static final String FIRST_INVOCATION = "firstInvocation";
    public static final String LAST_INVOCATION = "lastInvocation";
    public static final String COUNT = "count";
    public static final String HTTP_STATUSES = "httpStatuses";
    public static final String LATENCY_DISTRIBUTION = "latencyDistribution";
    public static final String RATE = "rate";
    public static final String PATHS = "paths";
    public static final String USER_AGENTS = "userAgents";

    public static final List<String> LATENCIES = List.of(

        "latency_0",
        "latency_1",
        "latency_2",
        "latency_3",
        "latency_4",
        "latency_5",
        "latency_6",
        "latency_7",
        "latency_8",
        "latency_n"
    );

    ///..
    // Dynamic properties
    public static final String ENABLED = "enabled";

    ///
}
