package io.github.clamentos.gattoslab.observability.metrics;

///
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.List;

///.
import org.bson.Document;
import org.bson.types.ObjectId;

///..
import org.springframework.stereotype.Component;

///..
import org.jspecify.annotations.NonNull;

///
@Component

///
public final class SystemMetrics {

    ///
    private final ThreadMXBean threadMXBean;
    private final MemoryMXBean memoryMXBean;
    private final OperatingSystemMXBean operatingSystemMXBean;
    private final ClassLoadingMXBean classLoadingMXBean;
    private final List<GarbageCollectorMXBean> garbageCollectorMXBeans;

    ///
    public SystemMetrics() {

        threadMXBean = ManagementFactory.getThreadMXBean();
        memoryMXBean = ManagementFactory.getMemoryMXBean();
        operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        classLoadingMXBean = ManagementFactory.getClassLoadingMXBean();
        garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
    }

    ///
    public @NonNull Document toDocument() {

        final int daemons = threadMXBean.getDaemonThreadCount();
        final Document document = new Document();

        document.append("_id", new ObjectId());
        document.append("timestamp", System.currentTimeMillis());
        document.append("heap", memoryMXBean.getHeapMemoryUsage().getUsed());
        document.append("nonHeap", memoryMXBean.getNonHeapMemoryUsage().getUsed());
        document.append("threads", threadMXBean.getThreadCount() - daemons);
        document.append("daemons", daemons);
        document.append("cpuLoadAvg", operatingSystemMXBean.getSystemLoadAverage());
        document.append("loadedClassCount", classLoadingMXBean.getLoadedClassCount());
        document.append("unloadedClassCount", classLoadingMXBean.getUnloadedClassCount());

        long totalCollectionTime = 0;
        long totalCollectionCount = 0;

        for(final GarbageCollectorMXBean bean : garbageCollectorMXBeans) {

            long collectionTime = bean.getCollectionTime();
            long collectionCount = bean.getCollectionCount();

            if(collectionTime != -1) totalCollectionTime += collectionTime;
            if(collectionCount != -1) totalCollectionCount += collectionCount;
        }

        document.append("totalGcTime", totalCollectionTime);
        document.append("totalGcCount", totalCollectionCount);

        return document;
    }

    ///
}
