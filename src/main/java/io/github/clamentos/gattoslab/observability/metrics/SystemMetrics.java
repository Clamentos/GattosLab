package io.github.clamentos.gattoslab.observability.metrics;

///
import io.github.clamentos.gattoslab.observability.metrics.system.MemoryInfo;
import io.github.clamentos.gattoslab.observability.metrics.system.MemoryInfoEntry;
import io.github.clamentos.gattoslab.observability.metrics.system.RuntimeInfo;
import io.github.clamentos.gattoslab.observability.metrics.system.SystemStatus;
import io.github.clamentos.gattoslab.observability.metrics.system.ThreadInfo;

///.
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;

///.
import org.springframework.stereotype.Component;

///
@Component

///
public final class SystemMetrics {

    ///
    private final ThreadMXBean threadMXBean;
    private final MemoryMXBean memoryMXBean;
    private final RuntimeMXBean runtimeMXBean;
    private final OperatingSystemMXBean operatingSystemMXBean;

    ///
    public SystemMetrics() {

        threadMXBean = ManagementFactory.getThreadMXBean();
        memoryMXBean = ManagementFactory.getMemoryMXBean();
        runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
    }

    ///
    public SystemStatus getJvmMetrics() {

        final RuntimeInfo runtimeInfo = new RuntimeInfo(

            runtimeMXBean.getStartTime(),
            runtimeMXBean.getUptime(),
            runtimeMXBean.getInputArguments(),
            runtimeMXBean.getSystemProperties()
        );

        final MemoryUsage heapUsage = memoryMXBean.getHeapMemoryUsage();
        final MemoryUsage nonHeapUsage = memoryMXBean.getNonHeapMemoryUsage();

        final MemoryInfoEntry heap = new MemoryInfoEntry(

            heapUsage.getInit(),
            heapUsage.getUsed(),
            heapUsage.getCommitted(),
            heapUsage.getMax()
        );

        final MemoryInfoEntry nonHeap = new MemoryInfoEntry(

            nonHeapUsage.getInit(),
            nonHeapUsage.getUsed(),
            nonHeapUsage.getCommitted(),
            nonHeapUsage.getMax()
        );

        final MemoryInfo memoryInfo = new MemoryInfo(heap, nonHeap);

        final int threadCount = threadMXBean.getThreadCount();
        final int daemonThreadCount = threadMXBean.getDaemonThreadCount();

        final ThreadInfo threadInfo = new ThreadInfo(

            threadCount - daemonThreadCount,
            daemonThreadCount,
            threadMXBean.getPeakThreadCount(),
            operatingSystemMXBean.getSystemLoadAverage()
        );

        return new SystemStatus(runtimeInfo, memoryInfo, threadInfo);
    }

    ///
}
