package io.github.clamentos.gattoslab.observability.metrics;

///
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;

///.
import org.bson.Document;
import org.bson.types.ObjectId;

///.
import org.springframework.stereotype.Component;

///
@Component

///
public final class SystemMetrics {

    ///
    private final ThreadMXBean threadMXBean;
    private final MemoryMXBean memoryMXBean;
    private final OperatingSystemMXBean operatingSystemMXBean;

    ///
    public SystemMetrics() {

        threadMXBean = ManagementFactory.getThreadMXBean();
        memoryMXBean = ManagementFactory.getMemoryMXBean();
        operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
    }

    ///
    public Document toDocument() {

        final int daemons = threadMXBean.getDaemonThreadCount();
        final Document document = new Document();

        document.append("_id", new ObjectId());
        document.append("timestamp", System.currentTimeMillis());
        document.append("heap", memoryMXBean.getHeapMemoryUsage().getUsed());
        document.append("nonHeap", memoryMXBean.getNonHeapMemoryUsage().getUsed());
        document.append("threads", threadMXBean.getThreadCount() - daemons);
        document.append("daemons", daemons);
        document.append("cpuLoadAvg", operatingSystemMXBean.getSystemLoadAverage());

        return document;
    }

    ///
}
