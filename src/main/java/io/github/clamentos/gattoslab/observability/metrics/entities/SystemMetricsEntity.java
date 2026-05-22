package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///..
import io.github.clamentos.gattoslab.observability.filters.SearchFilter;
import io.github.clamentos.gattoslab.persistence.SearchableEntity;

///..
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

///
@Getter
@Slf4j

///
public final class SystemMetricsEntity implements SearchableEntity {

    ///
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
    @JsonCreator
    public SystemMetricsEntity(

        @JsonProperty("timestamp") final long timestamp,
        @JsonProperty("platformThreads") final long platformThreads,
        @JsonProperty("classesLoaded") final long classesLoaded,
        @JsonProperty("fileReads") final long fileReads,
        @JsonProperty("fileWrites") final long fileWrites,
        @JsonProperty("socketReads") final long socketReads,
        @JsonProperty("socketWrites") final long socketWrites,
        @JsonProperty("gcCounts") final long gcCounts,
        @JsonProperty("gcPause") final long gcPause,
        @JsonProperty("cpuLoadJvmUser") final long cpuLoadJvmUser,
        @JsonProperty("cpuLoadJvmSystem") final long cpuLoadJvmSystem,
        @JsonProperty("cpuLoadMachineTotal") final long cpuLoadMachineTotal,
        @JsonProperty("systemMemoryUsed") final long systemMemoryUsed,
        @JsonProperty("metaSpaceUsed") final long metaSpaceUsed,
        @JsonProperty("directBuffersUsed") final long directBuffersUsed,
        @JsonProperty("directBuffersMemoryUsed") final long directBuffersMemoryUsed,
        @JsonProperty("heapUsed") final long heapUsed,
        @JsonProperty("storageUsed") final long storageUsed,
        @JsonProperty("requestMetricsEquilibrium") final long requestMetricsEquilibrium
    ) {

        this.timestamp = timestamp;
        this.platformThreads = platformThreads;
        this.classesLoaded = classesLoaded;
        this.fileReads = fileReads;
        this.fileWrites = fileWrites;
        this.socketReads = socketReads;
        this.socketWrites = socketWrites;
        this.gcCounts = gcCounts;
        this.gcPause = gcPause;
        this.cpuLoadJvmUser = cpuLoadJvmUser;
        this.cpuLoadJvmSystem = cpuLoadJvmSystem;
        this.cpuLoadMachineTotal = cpuLoadMachineTotal;
        this.systemMemoryUsed = systemMemoryUsed;
        this.metaSpaceUsed = metaSpaceUsed;
        this.directBuffersUsed = directBuffersUsed;
        this.directBuffersMemoryUsed = directBuffersMemoryUsed;
        this.heapUsed = heapUsed;
        this.storageUsed = storageUsed;
        this.requestMetricsEquilibrium = requestMetricsEquilibrium;
    }

    ///
    @Override
    public boolean respectsFilter(final SearchFilter searchFilter) {

        return timestamp >= searchFilter.getStartTimestamp() && timestamp <= searchFilter.getEndTimestamp();
    }

    ///..
    @Override
    public String toString() {

        return "{\"timestamp\":" + timestamp
                + ",\"platformThreads\":" + platformThreads + ",\"classesLoaded\":" + classesLoaded + ",\"fileReads\":"
                + fileReads + ",\"fileWrites\":" + fileWrites + ",\"socketReads\":" + socketReads + ",\"socketWrites\":"
                + socketWrites + ",\"gcCounts\":" + gcCounts + ",\"gcPause\":" + gcPause + ",\"cpuLoadJvmUser\":"
                + cpuLoadJvmUser + ",\"cpuLoadJvmSystem\":" + cpuLoadJvmSystem + ",\"cpuLoadMachineTotal\":"
                + cpuLoadMachineTotal + ",\"systemMemoryUsed\":" + systemMemoryUsed + ",\"metaSpaceUsed\":" + metaSpaceUsed
                + ",\"directBuffersUsed\":" + directBuffersUsed + ",\"directBuffersMemoryUsed\":" + directBuffersMemoryUsed
                + ",\"heapUsed\":" + heapUsed + ",\"storageUsed\":" + storageUsed + ",\"requestMetricsEquilibrium\":" + requestMetricsEquilibrium + "}";
    }

    ///
}
