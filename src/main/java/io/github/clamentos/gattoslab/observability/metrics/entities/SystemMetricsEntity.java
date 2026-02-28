package io.github.clamentos.gattoslab.observability.metrics.entities;

///
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

///..
import org.bson.types.ObjectId;

///
@AllArgsConstructor
@Getter
@Slf4j

///
public final class SystemMetricsEntity {

    ///
    private final ObjectId id;
    private final long timestamp;
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
    public SystemMetricsEntity(

        final long virtualThreads,
        final long platformThreads,
        final long classesLoaded,
        final long fileReads,
        final long fileWrites,
        final long socketReads,
        final long socketWrites,
        final long gcCounts,
        final long gcPause,
        final long cpuLoadJvmUser,
        final long cpuLoadJvmSystem,
        final long cpuLoadMachineTotal,
        final long systemMemoryUsed,
        final long metaSpaceUsed,
        final long directBuffersUsed,
        final long directBuffersMemoryUsed,
        final long heapUsed
    ) {

        id = new ObjectId();
        timestamp = System.currentTimeMillis();
        this.virtualThreads = virtualThreads;
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

        long storageUsedTmp = -1;

        try {

            final FileStore fileStore = Files.getFileStore(Paths.get("/"));
            storageUsedTmp = fileStore.getTotalSpace() - fileStore.getUnallocatedSpace();
        }

        catch(final IOException exc) {

            log.error("Could not get filesystem usage because", exc);
        }

        storageUsed = storageUsedTmp;
    }

    ///
}
