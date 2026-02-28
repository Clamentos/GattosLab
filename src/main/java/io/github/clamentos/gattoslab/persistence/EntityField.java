package io.github.clamentos.gattoslab.persistence;

///
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter

///
public enum EntityField {

    ///
    // Used by all
    ID("_id"),

    // Used by many
    TIMESTAMP("timestamp"),

    // LogEntity
    SEVERITY("severity"),
    THREAD("thread"),
    LOGGER("logger"),
    MESSAGE("message"),
    CLASS_NAME("className"),
    STACKTRACE("stacktrace"),
    EXCEPTION("exception"),

    // RequestMetricsEntity
    IS_OTHERS("isOthers"),
    PATH("path"),
    HTTP_STATUS("httpStatus"),
    USER_AGENT("userAgent"),
    LATENCY("latency"),

    // SystemMetricsEntity
    VIRTUAL_THREADS("virtualThreads"),
    PLATFORM_THREADS("platformThreads"),
    CLASSES_LOADED("classesLoaded"),
    FILE_READS("fileReads"),
    FILE_WRITES("fileWrites"),
    SOCKET_READS("socketReads"),
    SOCKET_WRITES("socketWrites"),
    GC_COUNTS("gcCounts"),
    GC_PAUSE("gcPause"),
    CPU_LOAD_JVM_USER("cpuLoadJvmUser"),
    CPU_LOAD_JVM_SYSTEM("cpuLoadJvmSystem"),
    CPU_LOAD_MACHINE_TOTAL("cpuLoadMachineTotal"),
    SYSTEM_MEMORY_USED("systemMemoryUsed"),
    META_SPACE_USED("metaSpaceUsed"),
    DIRECT_BUFFERS_USED("directBuffersUsed"),
    DIRECT_BUFFERS_MEMORY_USED("directBuffersMemoryUsed"),
    HEAP_USED("heapUsed"),
    STORAGE_USED("storageUsed"),

    // DynamicProperty
    KEY("key"),
    VALUE("value"),
    START("start"),
    END("end"),
    IPV4S("ipv4s"),
    IPV6S("ipv6s"),
    USER_AGENT_CONTAINS("userAgentContains");

    ///
    private final String field;

    ///
}
