package io.github.clamentos.gattoslab.observability.metrics;

///
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.observability.metrics.entities.SystemMetricsEntity;

///..
import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

///..
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingStream;

///
public final class SystemMetricsService implements Closeable {

    ///
    private final Logger logger;

    ///..
    private final ThreadMXBean threadMXBean;
    private final MemoryMXBean memoryMXBean;
    private final ClassLoadingMXBean classLoadingMXBean;
    private final RecordingStream recordingStream;
    private final FileStore fileStore;

    ///..
    private final AtomicLong systemMetricsIdCounter;

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

    private final AtomicLong requestMetricsEquilibrium;

    ///..
    private final AtomicLong sampleCounter;
    private final AtomicLong dumpCounter;

    ///
    public SystemMetricsService(final long samplingPeriod) throws IOException {

        this.logger = new Logger();

        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
        this.recordingStream = new RecordingStream();
        this.fileStore = Files.getFileStore(Paths.get("/"));

        this.systemMetricsIdCounter = new AtomicLong();

        this.fileReads = new AtomicLong();
        this.fileWrites = new AtomicLong();

        this.socketReads = new AtomicLong();
        this.socketWrites = new AtomicLong();

        this.gcCounts = new AtomicLong();
        this.gcPause = new AtomicLong();

        this.cpuLoadJvmUser = new AtomicLong();
        this.cpuLoadJvmSystem = new AtomicLong();
        this.cpuLoadMachineTotal = new AtomicLong();

        this.systemMemoryUsed = new AtomicLong();
        this.directBuffers = new AtomicLong();
        this.directBuffersMemoryUsed = new AtomicLong();

        this.requestMetricsEquilibrium = new AtomicLong();

        this.sampleCounter = new AtomicLong();
        this.dumpCounter = new AtomicLong();

        this.enableRecording("jdk.FileRead", _ -> this.fileReads.incrementAndGet());
        this.enableRecording("jdk.FileWrite", _ -> this.fileWrites.incrementAndGet());
        this.enableRecording("jdk.SocketWrite", _ -> this.socketReads.incrementAndGet());
        this.enableRecording("jdk.SocketWrite", _ -> this.socketWrites.incrementAndGet());

        this.enableRecording("jdk.GarbageCollection", event -> {

            this.gcCounts.incrementAndGet();
            this.gcPause.addAndGet(event.getDuration().get(ChronoUnit.NANOS) / 1_000_000);
        });

        this.enablePeriodicRecording("jdk.CPULoad", samplingPeriod, event -> {

            this.sampleCounter.incrementAndGet();

            this.cpuLoadJvmUser.set((long)(Math.ceil(event.getDouble("jvmUser") * 100)));
            this.cpuLoadJvmSystem.set((long)(Math.ceil(event.getDouble("jvmSystem") * 100)));
            this.cpuLoadMachineTotal.set((long)(Math.ceil(event.getDouble("machineTotal") * 100)));
        });

        this.enablePeriodicRecording("jdk.DirectBufferStatistics", samplingPeriod, event -> {

            this.directBuffers.set(event.getLong("count"));
            this.directBuffersMemoryUsed.set(event.getLong("memoryUsed"));
        });

        this.enablePeriodicRecording("jdk.PhysicalMemory", samplingPeriod, event -> this.systemMemoryUsed.set(event.getLong("usedSize")));
        this.recordingStream.startAsync();
    }

    ///
    public void requestStarted() {

        this.requestMetricsEquilibrium.incrementAndGet();
    }

    ///..
    public void requestMetricCreated() {

        this.requestMetricsEquilibrium.decrementAndGet();
    }

    ///..
    public SystemMetricsEntity sample() {

        final long sampleCounterValue = this.sampleCounter.get();

        if(this.dumpCounter.get() < sampleCounterValue) {

            this.dumpCounter.set(sampleCounterValue);
            long storageUsedTmp = -1;

            try {

                storageUsedTmp = fileStore.getTotalSpace() - fileStore.getUnallocatedSpace();
            }

            catch(final IOException exc) {

                this.logger.error("Could not get filesystem usage", exc);
            }

            return new SystemMetricsEntity(

                this.systemMetricsIdCounter.getAndIncrement(),
                System.currentTimeMillis(),
                this.threadMXBean.getThreadCount(),
                this.classLoadingMXBean.getLoadedClassCount(),
                this.fileReads.getAndSet(0),
                this.fileWrites.getAndSet(0),
                this.socketReads.getAndSet(0),
                this.socketWrites.getAndSet(0),
                this.gcCounts.get(),
                this.gcPause.getAndSet(0),
                this.cpuLoadJvmUser.getAndSet(0),
                this.cpuLoadJvmSystem.getAndSet(0),
                this.cpuLoadMachineTotal.getAndSet(0),
                this.systemMemoryUsed.getAndSet(0),
                this.memoryMXBean.getNonHeapMemoryUsage().getUsed(),
                this.directBuffers.getAndSet(0),
                this.directBuffersMemoryUsed.getAndSet(0),
                this.memoryMXBean.getHeapMemoryUsage().getUsed(),
                storageUsedTmp,
                this.requestMetricsEquilibrium.get()
            );
        }

        return null;
    }

    ///..
    @Override
    public void close() {

        this.logger.info("Begin shutdown...");
        this.recordingStream.close();
        this.logger.info("End shutdown");
    }

    ///.
    private void enableRecording(final String name, final Consumer<RecordedEvent> action) {

        this.recordingStream.enable(name).withoutStackTrace();
        this.recordingStream.onEvent(name, action);
    }

    ///..
    private void enablePeriodicRecording(final String name, final long samplingPeriod, final Consumer<RecordedEvent> action) {

        this.recordingStream.enable(name).withoutStackTrace().withPeriod(Duration.ofMillis(samplingPeriod));
        this.recordingStream.onEvent(name, action);
    }

    ///
}
