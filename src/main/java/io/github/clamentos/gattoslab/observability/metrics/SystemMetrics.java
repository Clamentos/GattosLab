package io.github.clamentos.gattoslab.observability.metrics;

///
import io.github.clamentos.gattoslab.observability.metrics.entities.SystemMetricsEntity;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

///..
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingStream;

///
public final class SystemMetrics implements Closeable {

    ///
    private final AtomicLong virtualThreads;
    private final AtomicLong platformThreads;

    private final AtomicLong classesLoaded;

    private final AtomicLong fileReads;
    private final AtomicLong fileWrites;

    private final AtomicLong socketReads;
    private final AtomicLong socketWrites;

    private final AtomicLong gcCounts;
    private final AtomicLong gcPause;

    private final AtomicLong cpuLoadJvmUser;
    private final AtomicLong cpuLoadJvmSystem;
    private final AtomicLong cpuLoadMachineTotal;

    private final AtomicLong systemMemoryUsed;
    private final AtomicLong directBuffers;
    private final AtomicLong directBuffersMemoryUsed;

    private final AtomicLong sampleCounter;
    private final AtomicLong dumpCounter;

    ///..
    private final MemoryMXBean memoryMXBean;
    private final RecordingStream recordingStream;

    ///
    public SystemMetrics(final long samplingPeriod) {

        virtualThreads = new AtomicLong();
        platformThreads = new AtomicLong();
        classesLoaded = new AtomicLong();
        fileReads = new AtomicLong();
        fileWrites = new AtomicLong();
        socketReads = new AtomicLong();
        socketWrites = new AtomicLong();
        gcCounts = new AtomicLong();
        gcPause = new AtomicLong();
        cpuLoadJvmUser = new AtomicLong();
        cpuLoadJvmSystem = new AtomicLong();
        cpuLoadMachineTotal = new AtomicLong();
        systemMemoryUsed = new AtomicLong();
        directBuffers = new AtomicLong();
        directBuffersMemoryUsed = new AtomicLong();

        sampleCounter = new AtomicLong();
        dumpCounter = new AtomicLong();

        memoryMXBean = ManagementFactory.getMemoryMXBean();
        recordingStream = new RecordingStream();

        this.enableRecording("jdk.VirtualThreadStart", _ -> virtualThreads.incrementAndGet());
        this.enableRecording("jdk.VirtualThreadEnd", _ -> virtualThreads.decrementAndGet());
        this.enableRecording("jdk.ThreadStart", _ -> platformThreads.incrementAndGet());
        this.enableRecording("jdk.ThreadEnd", _ -> platformThreads.decrementAndGet());

        this.enableRecording("jdk.ClassLoad", _ -> classesLoaded.incrementAndGet());
        this.enableRecording("jdk.ClassUnload", _ -> classesLoaded.decrementAndGet());

        this.enableRecording("jdk.FileRead", _ -> fileReads.incrementAndGet());
        this.enableRecording("jdk.FileWrite", _ -> fileWrites.incrementAndGet());
        this.enableRecording("jdk.SocketWrite", _ -> socketReads.incrementAndGet());
        this.enableRecording("jdk.SocketWrite", _ -> socketWrites.incrementAndGet());

        this.enableRecording("jdk.GarbageCollection", event -> {

            gcCounts.incrementAndGet();
            gcPause.addAndGet(event.getDuration().get(ChronoUnit.NANOS) / 1000000);
        });

        this.enablePeriodicRecording("jdk.CPULoad", samplingPeriod, event -> {

            sampleCounter.incrementAndGet();

            cpuLoadJvmUser.set((long)(event.getDouble("jvmUser") * 100));
            cpuLoadJvmSystem.set((long)(event.getDouble("jvmSystem") * 100));
            cpuLoadMachineTotal.set((long)(event.getDouble("machineTotal") * 100));
        });

        this.enablePeriodicRecording("jdk.DirectBufferStatistics", samplingPeriod, event -> {

            directBuffers.set(event.getLong("count"));
            directBuffersMemoryUsed.set(event.getLong("memoryUsed"));
        });

        this.enablePeriodicRecording("jdk.PhysicalMemory", samplingPeriod, event -> systemMemoryUsed.set(event.getLong("usedSize")));
        recordingStream.startAsync();
    }

    ///
    public SystemMetricsEntity toEntity() {

        final long currentDump = dumpCounter.incrementAndGet();
        while(currentDump != sampleCounter.get()) GenericUtils.sleep(100L);

        final SystemMetricsEntity entity = new SystemMetricsEntity(

            virtualThreads.get(),
            platformThreads.get(),
            classesLoaded.get(),
            fileReads.get(),
            fileWrites.get(),
            socketReads.get(),
            socketWrites.get(),
            gcCounts.get(),
            gcPause.get(),
            cpuLoadJvmUser.get(),
            cpuLoadJvmSystem.get(),
            cpuLoadMachineTotal.get(),
            systemMemoryUsed.get(),
            memoryMXBean.getNonHeapMemoryUsage().getUsed(),
            directBuffers.get(),
            directBuffersMemoryUsed.get(),
            memoryMXBean.getHeapMemoryUsage().getUsed()
        );

        fileReads.set(0);
        fileWrites.set(0);
        socketReads.set(0);
        socketWrites.set(0);
        cpuLoadJvmUser.set(0);
        cpuLoadJvmSystem.set(0);
        cpuLoadMachineTotal.set(0);
        systemMemoryUsed.set(0);
        directBuffers.set(0);
        directBuffersMemoryUsed.set(0);

        return entity;
    }

    ///..
    @Override
    public void close() throws IOException {

        recordingStream.close();
    }

    ///.
    private void enableRecording(final String name, final Consumer<RecordedEvent> action) {

        recordingStream.enable(name).withoutStackTrace();
        recordingStream.onEvent(name, action);
    }

    ///..
    private void enablePeriodicRecording(final String name, final long samplingPeriod, final Consumer<RecordedEvent> action) {

        recordingStream.enable(name).withoutStackTrace().withPeriod(Duration.ofMillis(samplingPeriod));
        recordingStream.onEvent(name, action);
    }

    ///
}
