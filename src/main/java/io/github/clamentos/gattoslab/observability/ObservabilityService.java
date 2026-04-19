package io.github.clamentos.gattoslab.observability;

///
import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.pojos.MetricsConfig;
import io.github.clamentos.gattoslab.eventbus.EventBus;
import io.github.clamentos.gattoslab.exceptions.ValidationException;
import io.github.clamentos.gattoslab.http.HttpUtils;
import io.github.clamentos.gattoslab.http.ResponseSender;
import io.github.clamentos.gattoslab.observability.filters.AggregationPipelines;
import io.github.clamentos.gattoslab.observability.filters.RequestMetricsSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.SystemMetricsSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.TemporalSearchFilter;
import io.github.clamentos.gattoslab.observability.metrics.ObservabilityContext;
import io.github.clamentos.gattoslab.observability.metrics.SystemMetrics;
import io.github.clamentos.gattoslab.observability.metrics.entities.PathInvocationAggregationEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.RequestMetricsAggregateEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.RequestMetricsEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.SystemMetricsAggregateEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.SystemMetricsEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.UserAgentAggregationEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.charts.BubbleChartDataEntry;
import io.github.clamentos.gattoslab.observability.metrics.entities.charts.ChartDataset;
import io.github.clamentos.gattoslab.observability.metrics.entities.charts.RequestMetricsCharts;
import io.github.clamentos.gattoslab.observability.metrics.entities.charts.SystemMetricsCharts;
import io.github.clamentos.gattoslab.observability.metrics.entities.charts.BubbleChart;
import io.github.clamentos.gattoslab.observability.metrics.entities.charts.LineChart;
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.persistence.EntityField;
import io.github.clamentos.gattoslab.persistence.MongoClientWrapper;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.website.Website;

///..
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

///..
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

///..
import lombok.extern.slf4j.Slf4j;

///..
import org.bson.conversions.Bson;

///..
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;

///
@Slf4j

///
public class ObservabilityService implements Closeable {

    ///
    private final int siphonCapacity;
    private final long requestMetricsRetention;
    private final long systemMetricsRetention;
    private final Set<String> monitoredPaths;

    ///..
    private final MongoClientWrapper mongoClientWrapper;

    ///..
    private final SystemMetrics systemMetrics;
    private final EventBus eventBus;
    private final AtomicBoolean isHandlingEvent;

    private final AtomicReference<ObservabilityContext> primaryContext;
    private final AtomicReference<ObservabilityContext> secondaryContext;

    private final Queue<ObservabilityContext> requestMetricsDumpFailures;
    private final Queue<SystemMetricsEntity> systemMetricsDumpFailures;

    ///
    public ObservabilityService(

        final ApplicationProperties applicationProperties,
        final BatchScheduler batchScheduler,
        final Website website,
        final MongoClientWrapper mongoClientWrapper

    ) throws IllegalArgumentException {

        eventBus = new EventBus(this::dumpMetrics);
        final MetricsConfig metricsConfig = applicationProperties.getMetricsConfig();

        final long systemMetricsSamplingPeriod = batchScheduler.schedule(

            this::sampleSystemMetrics,
            "ObservabilityService::sampleSystemMetrics",
            metricsConfig.getSystemMetricsSampling()

        );

        batchScheduler.schedule(eventBus::trigger, "ObservabilityService::trigger", metricsConfig.getDumpToDbSchedule());
        batchScheduler.schedule(this::deleteOldMetrics, "ObservabilityService::deleteOldMetrics", metricsConfig.getRetentionSchedule());

        systemMetrics = new SystemMetrics(systemMetricsSamplingPeriod);

        siphonCapacity = metricsConfig.getSiphonCapacity();
        requestMetricsRetention = metricsConfig.getRequestMetricsRetention().toMillis();
        systemMetricsRetention = metricsConfig.getSystemMetricsRetention().toMillis();
        monitoredPaths = website.getPaths();

        this.mongoClientWrapper = mongoClientWrapper;

        primaryContext = new AtomicReference<>(new ObservabilityContext(eventBus, siphonCapacity));
        secondaryContext = new AtomicReference<>(new ObservabilityContext(eventBus, siphonCapacity));

        requestMetricsDumpFailures = new ConcurrentLinkedQueue<>();
        systemMetricsDumpFailures = new ConcurrentLinkedQueue<>();

        isHandlingEvent = new AtomicBoolean();
    }

    ///
    public ResponseSender getRequestMetrics(final JsonGenerator generator, final RequestMetricsSearchFilter searchFilter)
    throws JacksonException, MongoException, ValidationException {

        this.validateTemporalFilter(searchFilter);

        final long bucketSize = searchFilter.getBucketSize();
        final MongoCollection<RequestMetricsEntity> collection = mongoClientWrapper.getCollection(DatabaseCollection.REQUEST_METRICS);
        final Map<String, Map<Long, RequestMetricsAggregateEntity>> metricsMap = new HashMap<>();

        final MongoCursor<RequestMetricsAggregateEntity> entityCursor = collection.aggregate(

            AggregationPipelines.performanceMetricsPipeline(searchFilter, bucketSize), 
            RequestMetricsAggregateEntity.class

        ).iterator();

        long minSlot = Long.MAX_VALUE;
        long maxSlot = Long.MIN_VALUE;

        try(entityCursor) {

            while(entityCursor.hasNext()) {

                final RequestMetricsAggregateEntity entity = entityCursor.next();
                final long timeSlot = entity.getTimeSlot();

                metricsMap.computeIfAbsent(entity.getKey(), _ -> new TreeMap<>()).put(timeSlot * bucketSize, entity);
                minSlot = Math.min(minSlot, timeSlot);
                maxSlot = Math.max(maxSlot, timeSlot);
            }
        }

        final long startTimestampAligned = minSlot * bucketSize;
        final long endTimestampAligned = maxSlot * bucketSize;
        final long[] labels = new long[(int)((endTimestampAligned - startTimestampAligned) / bucketSize) + 1];

        for(final Map<Long, RequestMetricsAggregateEntity> metricsMapInner : metricsMap.values()) {

            for(int i = 0; i < labels.length; i++) {

                final long timestamp = (i * bucketSize) + startTimestampAligned;

                labels[i] = timestamp;
                metricsMapInner.putIfAbsent(timestamp, null);
            }
        }

        final List<ChartDataset<long[]>> rateDatasets = new ArrayList<>();
        final List<ChartDataset<List<BubbleChartDataEntry>>> latencyDatasets = new ArrayList<>();

        for(final Map.Entry<String, Map<Long, RequestMetricsAggregateEntity>> metricsMapEntry : metricsMap.entrySet()) {

            final String key = metricsMapEntry.getKey();
            final Collection<RequestMetricsAggregateEntity> innerEntities = metricsMapEntry.getValue().values();
            final long[] rateData = new long[innerEntities.size()];
            final List<BubbleChartDataEntry> latencyData = new ArrayList<>();

            int index = 0;

            for(final RequestMetricsAggregateEntity entity : innerEntities) {

                if(entity != null) {

                    rateData[index] = entity.getRate();
                    final int[] latencyDistribution = entity.getLatencyDistribution();

                    for(int i = 0; i < latencyDistribution.length; i++) {

                        latencyData.add(new BubbleChartDataEntry(entity.getTimeSlot(), i, latencyDistribution[i]));
                    }
                }

                else {

                    rateData[index] = 0;
                }

                index++;
            }

            rateDatasets.add(new ChartDataset<>(key, rateData));
            latencyDatasets.add(new ChartDataset<>(key, latencyData));
        }

        return () -> generator.writePOJO(new RequestMetricsCharts(new LineChart(labels, rateDatasets), new BubbleChart(latencyDatasets)));
    }

    ///..
    public ResponseSender getInvocationMetrics(final JsonGenerator generator, final RequestMetricsSearchFilter searchFilter)
    throws JacksonException, MongoException, ValidationException {

        this.validateTemporalFilter(searchFilter);

        final MongoCollection<RequestMetricsEntity> collection = mongoClientWrapper.getCollection(DatabaseCollection.REQUEST_METRICS);
        final List<Bson> pathsAggregation = AggregationPipelines.pathMetricsPipeline(searchFilter);
        final List<Bson> userAgentsAggregation = AggregationPipelines.userAgentMetricsPipeline(searchFilter);

        return () -> {

            try(
                final MongoCursor<PathInvocationAggregationEntity> paths = collection.aggregate(pathsAggregation, PathInvocationAggregationEntity.class).iterator();
                final MongoCursor<UserAgentAggregationEntity> userAgents = collection.aggregate(userAgentsAggregation, UserAgentAggregationEntity.class).iterator();
            ) {

                generator.writeStartObject();

                generator.writeArrayPropertyStart(EntityField.PATHS);
                while(paths.hasNext()) generator.writePOJO(paths.next());
                generator.writeEndArray();

                generator.writeArrayPropertyStart(EntityField.USER_AGENTS);
                while(userAgents.hasNext()) generator.writePOJO(userAgents.next());
                generator.writeEndArray();

                generator.writeEndObject();
            }
        };
    }

    ///..
    public ResponseSender getSystemMetrics(final JsonGenerator generator, final SystemMetricsSearchFilter searchFilter)
    throws JacksonException, MongoException, ValidationException {

        this.validateTemporalFilter(searchFilter);

        final long bucketSize = searchFilter.getBucketSize();
        final MongoCollection<SystemMetricsEntity> collection = mongoClientWrapper.getCollection(DatabaseCollection.SYSTEM_METRICS);
        final Map<Long, SystemMetricsAggregateEntity> metricsMap = new TreeMap<>();

        final MongoCursor<SystemMetricsAggregateEntity> metrics = collection.aggregate(

            AggregationPipelines.systemMetricsPipeline(searchFilter, bucketSize),
            SystemMetricsAggregateEntity.class

        ).iterator();

        long minSlot = Long.MAX_VALUE;
        long maxSlot = Long.MIN_VALUE;

        try(metrics) {

            while(metrics.hasNext()) {

                final SystemMetricsAggregateEntity metric = metrics.next();
                final long timeSlot = metric.getTimeSlot();

                metricsMap.put(timeSlot, metric);
                maxSlot = Math.max(maxSlot, timeSlot);
                minSlot = Math.min(minSlot, timeSlot);
            }
        }

        final long startTimestampAligned = minSlot * bucketSize;
        final long endTimestampAligned = maxSlot * bucketSize;
        final long[] labels = new long[(int)((endTimestampAligned - startTimestampAligned) / bucketSize)];

        for(int i = 0; i < labels.length; i++) {

            final long timestamp = (i * bucketSize) + startTimestampAligned;

            labels[i] = timestamp;
            metricsMap.putIfAbsent(timestamp, null);
        }

        final long[] virtualThreads = new long[labels.length];
        final long[] platformThreads = new long[labels.length];
        final long[] classes = new long[labels.length];
        final long[] fileReads = new long[labels.length];
        final long[] fileWrites = new long[labels.length];
        final long[] socketReads = new long[labels.length];
        final long[] socketWrites = new long[labels.length];
        final long[] gcCounts = new long[labels.length];
        final long[] gcPause = new long[labels.length];
        final long[] cpuUser = new long[labels.length];
        final long[] cpuSystem = new long[labels.length];
        final long[] cpuMachine = new long[labels.length];
        final long[] systemMemoryUsed = new long[labels.length];
        final long[] metaSpaceUsed = new long[labels.length];
        final long[] directBuffersUsed = new long[labels.length];
        final long[] directBuffersMemoryUsed = new long[labels.length];
        final long[] heapUsed = new long[labels.length];
        final long[] storageUsed = new long[labels.length];

        int index = 0;

        for(final SystemMetricsAggregateEntity aggregate : metricsMap.values()) {

            if(aggregate != null) {

                virtualThreads[index] = aggregate.getVirtualThreads();
                platformThreads[index] = aggregate.getPlatformThreads();
                classes[index] = aggregate.getClassesLoaded();
                fileReads[index] = aggregate.getFileReads();
                fileWrites[index] = aggregate.getFileWrites();
                socketReads[index] = aggregate.getSocketReads();
                socketWrites[index] = aggregate.getSocketWrites();
                gcCounts[index] = aggregate.getGcCounts();
                gcPause[index] = aggregate.getGcPause();
                cpuUser[index] = aggregate.getCpuLoadJvmUser();
                cpuSystem[index] = aggregate.getCpuLoadJvmSystem();
                cpuMachine[index] = aggregate.getCpuLoadMachineTotal();
                systemMemoryUsed[index] = aggregate.getSystemMemoryUsed();
                metaSpaceUsed[index] = aggregate.getMetaSpaceUsed();
                directBuffersUsed[index] = aggregate.getDirectBuffersUsed();
                directBuffersMemoryUsed[index] = aggregate.getDirectBuffersMemoryUsed();
                heapUsed[index] = aggregate.getHeapUsed();
                storageUsed[index] = aggregate.getStorageUsed();
            }

            else {

                virtualThreads[index] = 0;
                platformThreads[index] = 0;
                classes[index] = 0;
                fileReads[index] = 0;
                fileWrites[index] = 0;
                socketReads[index] = 0;
                socketWrites[index] = 0;
                gcCounts[index] = 0;
                gcPause[index] = 0;
                cpuUser[index] = 0;
                cpuSystem[index] = 0;
                cpuMachine[index] = 0;
                systemMemoryUsed[index] = 0;
                metaSpaceUsed[index] = 0;
                directBuffersUsed[index] = 0;
                directBuffersMemoryUsed[index] = 0;
                heapUsed[index] = 0;
                storageUsed[index] = 0;
            }

            index++;
        }

        return () -> generator.writePOJO(new SystemMetricsCharts(

            new LineChart(labels, List.of(new ChartDataset<>(EntityField.VIRTUAL_THREADS, virtualThreads), new ChartDataset<>(EntityField.PLATFORM_THREADS, platformThreads))),
            new LineChart(labels, List.of(new ChartDataset<>(EntityField.CLASSES_LOADED, classes))),

            new LineChart(labels, List.of(

                new ChartDataset<>(EntityField.FILE_READS, fileReads),
                new ChartDataset<>(EntityField.FILE_WRITES, fileWrites),
                new ChartDataset<>(EntityField.SOCKET_READS, socketReads),
                new ChartDataset<>(EntityField.SOCKET_WRITES, socketWrites)
            )),

            new LineChart(labels, List.of(new ChartDataset<>(EntityField.GC_COUNTS, gcCounts), new ChartDataset<>(EntityField.GC_PAUSE, gcPause))),

            new LineChart(labels, List.of(

                new ChartDataset<>(EntityField.CPU_LOAD_JVM_USER, cpuUser),
                new ChartDataset<>(EntityField.CPU_LOAD_JVM_SYSTEM, cpuSystem),
                new ChartDataset<>(EntityField.CPU_LOAD_MACHINE_TOTAL, cpuMachine)
            )),

            new LineChart(labels, List.of(

                new ChartDataset<>(EntityField.SYSTEM_MEMORY_USED, systemMemoryUsed),
                new ChartDataset<>(EntityField.META_SPACE_USED, metaSpaceUsed),
                new ChartDataset<>(EntityField.DIRECT_BUFFERS_USED, directBuffersUsed),
                new ChartDataset<>(EntityField.DIRECT_BUFFERS_MEMORY_USED, directBuffersMemoryUsed),
                new ChartDataset<>(EntityField.HEAP_USED, heapUsed)
            )),

            new LineChart(labels, List.of(new ChartDataset<>(EntityField.STORAGE_USED, storageUsed)))
        ));
    }

    ///..
    public void updateRequestMetrics(final HttpServerExchange exchange) {

        final String path = exchange.getRequestPath();

        while(true) {

            final boolean success = primaryContext.get().updateMetrics(

                path,
                HttpUtils.getHeaderValue(exchange.getRequestHeaders(), Headers.USER_AGENT_STRING),
                !monitoredPaths.contains(path),
                exchange.getAttachment(HttpUtils.START_TIME_EPOCH_MS),
                exchange.getStatusCode()
            );

            if(!success) {

                try {

                    Thread.sleep(1L);
                }

                catch(final InterruptedException _) {

                    Thread.currentThread().interrupt();
                    break;
                }
            }

            else {

                break;
            }
        }
	}

    ///..
    public void close() throws IOException {

        log.info("Begin shutdown...");

        this.dumpMetrics();
        systemMetrics.close();

        log.info("End shutdown");
    }

    ///.
    private void sampleSystemMetrics() {

        final ClientSession session = mongoClientWrapper.getClient().startSession();
        SystemMetricsEntity toSave = systemMetrics.toEntity();

        try {

            session.startTransaction();
            mongoClientWrapper.getCollection(DatabaseCollection.SYSTEM_METRICS).insertOne(toSave);

            toSave = systemMetricsDumpFailures.poll();
            if(toSave != null) mongoClientWrapper.getCollection(DatabaseCollection.SYSTEM_METRICS).insertOne(toSave);

            session.commitTransaction();
        }

        catch(final Exception exc) {

            log.error("Could not write metrics to DB", exc);

            session.abortTransaction();
            systemMetricsDumpFailures.add(toSave);
        }

        session.close();
    }

    ///..
    private void deleteOldMetrics() {

        log.info("Begin delete metrics by retention");
        final ClientSession session = mongoClientWrapper.getClient().startSession();

        long requestsDeleted = 0;
        long systemsDeleted = 0;

        try {

            final long now = System.currentTimeMillis();
            final Bson requestDeleteFilter = Filters.lte(EntityField.TIMESTAMP, now - requestMetricsRetention);
            final Bson systemDeleteFilter = Filters.lte(EntityField.TIMESTAMP, now - systemMetricsRetention);

            session.startTransaction();

            requestsDeleted = mongoClientWrapper.getCollection(DatabaseCollection.REQUEST_METRICS).deleteMany(requestDeleteFilter).getDeletedCount();
            systemsDeleted = mongoClientWrapper.getCollection(DatabaseCollection.SYSTEM_METRICS).deleteMany(systemDeleteFilter).getDeletedCount();

            session.commitTransaction();
        }

        catch(final Exception exc) {

            log.error("Could not delete old metrics from DB", exc);
            session.abortTransaction();
        }

        log.info("End delete metrics by retention, deleted {} request metrics and {} system metrics", requestsDeleted, systemsDeleted);
        session.close();
    }

    ///..
    private void dumpMetrics() {

        if(isHandlingEvent.compareAndSet(false, true)) {

            final ClientSession session = mongoClientWrapper.getClient().startSession();
            ObservabilityContext oldPrimary = this.swapContexts();

            try {

                session.startTransaction();
                this.insertMetrics(oldPrimary);

                oldPrimary = requestMetricsDumpFailures.poll();
                if(oldPrimary != null) this.insertMetrics(oldPrimary);

                session.commitTransaction();
            }

            catch(final Exception exc) {

                log.error("Could not write metrics to DB", exc);

                session.abortTransaction();
                requestMetricsDumpFailures.add(oldPrimary);
                secondaryContext.set(new ObservabilityContext(eventBus, siphonCapacity));
            }

            session.close();
            isHandlingEvent.set(false);
        }
    }

    ///..
    private ObservabilityContext swapContexts() {

        final ObservabilityContext primary = primaryContext.get();

        primaryContext.set(secondaryContext.get());
        secondaryContext.set(primary);

        return primary;
    }

    ///..
    private void insertMetrics(final ObservabilityContext context) throws MongoException {

        while(!context.isNoOneThere()) {

            try {

                Thread.sleep(1L);
            }

            catch(final InterruptedException _) {

                Thread.currentThread().interrupt();
                break;
            }
        }

        mongoClientWrapper.insertAll(context.drainSiphon(), DatabaseCollection.REQUEST_METRICS);
        context.reset();
    }

    ///..
    private void validateTemporalFilter(final TemporalSearchFilter temporalSearchFilter) throws ValidationException {

        final long startTimestamp = temporalSearchFilter.getStartTimestamp();
        final long endTimestamp = temporalSearchFilter.getEndTimestamp();

        if(startTimestamp > endTimestamp) throw new ValidationException("endTimestamp cannot be smaller than startTimestamp");
    }

    ///
}
