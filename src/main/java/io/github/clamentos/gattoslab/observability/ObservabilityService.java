package io.github.clamentos.gattoslab.observability;

///
import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.eventbus.EventBus;
import io.github.clamentos.gattoslab.exceptions.ValidationException;
import io.github.clamentos.gattoslab.http.HttpUtils;
import io.github.clamentos.gattoslab.http.ResponseSender;
import io.github.clamentos.gattoslab.observability.filters.AggregatedSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.AggregationPipelines;
import io.github.clamentos.gattoslab.observability.filters.RequestMetricsSearchFilter;
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
import io.github.clamentos.gattoslab.scheduling.SimpleCron;
import io.github.clamentos.gattoslab.utils.GenericUtils;
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
    private static final int MAX_TIMESTAMPS = 10000;
    private static final String SOURCE_VALIDATE = "ObservabilityService.validateSearchFilter";

    ///..
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
        systemMetrics = new SystemMetrics(SimpleCron.decodePeriod(applicationProperties.getSystemMetricsSampling()));

        batchScheduler.schedule(this::sampleSystemMetrics, "ObservabilityService::sampleSystemMetrics", applicationProperties.getSystemMetricsPolling());
        batchScheduler.schedule(eventBus::trigger, "ObservabilityService::trigger", applicationProperties.getMetricsDumpToDbSchedule());
        batchScheduler.schedule(this::deleteOldMetrics, "ObservabilityService::deleteOldMetrics", applicationProperties.getMetricsRetentionSchedule());

        siphonCapacity = applicationProperties.getMetricsSiphonCapacity();
        requestMetricsRetention = applicationProperties.getRequestMetricsRetention().toMillis();
        systemMetricsRetention = applicationProperties.getSystemMetricsRetention().toMillis();
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

        this.validateSearchFilter(searchFilter, true);

        final MongoCollection<RequestMetricsEntity> collection = mongoClientWrapper.getCollection(DatabaseCollection.REQUEST_METRICS);
        final long bucketSize = searchFilter.getBucketSize();

        final MongoCursor<RequestMetricsAggregateEntity> entityCursor = collection.aggregate(

            AggregationPipelines.performanceMetricsPipeline(searchFilter), 
            RequestMetricsAggregateEntity.class

        ).iterator();

        final Map<String, Map<Long, RequestMetricsAggregateEntity>> metricsMap = new HashMap<>();

        try(entityCursor) {

            while(entityCursor.hasNext()) {

                final RequestMetricsAggregateEntity entity = entityCursor.next();
                final long timeSlot = entity.getTimeSlot();

                metricsMap.computeIfAbsent(entity.getKey(), _ -> new TreeMap<>()).put(timeSlot * bucketSize, entity);
            }
        }

        final long[] labels = new long[(int)((searchFilter.getEndTimestamp() - searchFilter.getStartTimestamp()) / bucketSize) + 1];

        for(int i = 0; i < labels.length; i++) {

            labels[i] = (i * bucketSize) + searchFilter.getStartTimestamp();
        }

        for(final Map<Long, RequestMetricsAggregateEntity> metricsMapInner : metricsMap.values()) {

            for(int i = 0; i < labels.length; i++) {

                metricsMapInner.putIfAbsent(labels[i], null);
            }
        }

        final List<ChartDataset<long[]>> rateDatasets = new ArrayList<>();
        final List<ChartDataset<List<BubbleChartDataEntry>>> latencyDatasets = new ArrayList<>();

        for(final Map.Entry<String, Map<Long, RequestMetricsAggregateEntity>> metricsMapEntry : metricsMap.entrySet()) {

            final List<BubbleChartDataEntry> latencyData = new ArrayList<>();
            final Collection<RequestMetricsAggregateEntity> innerEntities = metricsMapEntry.getValue().values();
            final long[] rateData = new long[innerEntities.size()];

            int index = 0;

            for(final RequestMetricsAggregateEntity entity : innerEntities) {

                if(entity != null) {

                    rateData[index] = entity.getRate();
                    final int[] latencyDistribution = entity.getLatencyDistribution();

                    for(int i = 0; i < latencyDistribution.length; i++) {

                        latencyData.add(new BubbleChartDataEntry(entity.getTimeSlot() * bucketSize, i, latencyDistribution[i]));
                    }
                }

                else {

                    rateData[index] = 0;
                }

                index++;
            }

            final String key = metricsMapEntry.getKey();

            rateDatasets.add(new ChartDataset<>(key, rateData));
            latencyDatasets.add(new ChartDataset<>(key, latencyData));
        }

        return () -> generator.writePOJO(new RequestMetricsCharts(new LineChart(labels, rateDatasets), new BubbleChart(latencyDatasets)));
    }

    ///..
    public ResponseSender getInvocationMetrics(final JsonGenerator generator, final RequestMetricsSearchFilter searchFilter)
    throws JacksonException, MongoException, ValidationException {

        this.validateSearchFilter(searchFilter, false);

        final MongoCollection<RequestMetricsEntity> collection = mongoClientWrapper.getCollection(DatabaseCollection.REQUEST_METRICS);
        final List<Bson> pathsAggregation = AggregationPipelines.invocationMetricsPipeline(searchFilter);
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
    public ResponseSender getSystemMetrics(final JsonGenerator generator, final AggregatedSearchFilter searchFilter)
    throws JacksonException, MongoException, ValidationException {

        this.validateSearchFilter(searchFilter, true);

        final long bucketSize = searchFilter.getBucketSize();
        final MongoCollection<SystemMetricsEntity> collection = mongoClientWrapper.getCollection(DatabaseCollection.SYSTEM_METRICS);

        final MongoCursor<SystemMetricsAggregateEntity> metrics = collection.aggregate(

            AggregationPipelines.systemMetricsPipeline(searchFilter),
            SystemMetricsAggregateEntity.class

        ).iterator();

        final Map<Long, SystemMetricsAggregateEntity> metricsMap = new TreeMap<>();

        try(metrics) {

            while(metrics.hasNext()) {

                final SystemMetricsAggregateEntity metric = metrics.next();
                final long timeSlot = metric.getTimeSlot();

                metricsMap.put(timeSlot * bucketSize, metric);
            }
        }

        final long[] labels = new long[(int)((searchFilter.getEndTimestamp() - searchFilter.getStartTimestamp()) / bucketSize) + 1];

        for(int i = 0; i < labels.length; i++) {

            labels[i] = (i * bucketSize) + searchFilter.getStartTimestamp();
            metricsMap.putIfAbsent(labels[i], null);
        }

        final int actualSize = metricsMap.size();

        final long[] virtualThreads = new long[actualSize];
        final long[] platformThreads = new long[actualSize];
        final long[] classes = new long[actualSize];
        final long[] fileReads = new long[actualSize];
        final long[] fileWrites = new long[actualSize];
        final long[] socketReads = new long[actualSize];
        final long[] socketWrites = new long[actualSize];
        final long[] gcCounts = new long[actualSize];
        final long[] gcPause = new long[actualSize];
        final long[] cpuUser = new long[actualSize];
        final long[] cpuSystem = new long[actualSize];
        final long[] cpuMachine = new long[actualSize];
        final long[] systemMemoryUsed = new long[actualSize];
        final long[] metaSpaceUsed = new long[actualSize];
        final long[] directBuffersUsed = new long[actualSize];
        final long[] directBuffersMemoryUsed = new long[actualSize];
        final long[] heapUsed = new long[actualSize];
        final long[] storageUsed = new long[actualSize];

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

            new LineChart(labels, List.of(new ChartDataset<>("Virtual", virtualThreads), new ChartDataset<>("Platform", platformThreads))),
            new LineChart(labels, List.of(new ChartDataset<>("Loaded classes", classes))),

            new LineChart(labels, List.of(

                new ChartDataset<>("File reads", fileReads),
                new ChartDataset<>("File writes", fileWrites),
                new ChartDataset<>("Socket reads", socketReads),
                new ChartDataset<>("Socket writes", socketWrites)
            )),

            new LineChart(labels, List.of(new ChartDataset<>("GC count", gcCounts), new ChartDataset<>("GC pause (ms)", gcPause))),

            new LineChart(labels, List.of(

                new ChartDataset<>("JVM user", cpuUser),
                new ChartDataset<>("JVM system", cpuSystem),
                new ChartDataset<>("Machine total", cpuMachine)
            )),

            new LineChart(labels, List.of(

                new ChartDataset<>("System memory total", systemMemoryUsed),
                new ChartDataset<>("Metaspace used", metaSpaceUsed),
                new ChartDataset<>("Direct buffers count", directBuffersUsed),
                new ChartDataset<>("Direct buffers used", directBuffersMemoryUsed),
                new ChartDataset<>("Heap used", heapUsed)
            )),

            new LineChart(labels, List.of(new ChartDataset<>("Storage used", storageUsed)))
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

            if(!success) GenericUtils.silentSleep(1L);
            else break;
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

        SystemMetricsEntity toSave = systemMetrics.toEntity();

        if(toSave != null) {

            final ClientSession session = mongoClientWrapper.getClient().startSession();

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
            log.info("End delete metrics by retention, deleted {} request metrics and {} system metrics", requestsDeleted, systemsDeleted);
        }

        catch(final Exception exc) {

            log.error("Could not delete old metrics from DB", exc);
            session.abortTransaction();
        }

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

        while(!context.isNoOneThere()) GenericUtils.silentSleep(1L);

        mongoClientWrapper.insertAll(context.drainSiphon(), DatabaseCollection.REQUEST_METRICS);
        context.reset();
    }

    ///..
    private void validateSearchFilter(final TemporalSearchFilter temporalSearchFilter, final boolean isBucketRequired) throws ValidationException {

        final long startTimestamp = temporalSearchFilter.getStartTimestamp();
        final long endTimestamp = temporalSearchFilter.getEndTimestamp();

        if(startTimestamp > endTimestamp) throw new ValidationException("Field 'endTimestamp' cannot be smaller than 'startTimestamp'", SOURCE_VALIDATE);

        if((temporalSearchFilter instanceof final AggregatedSearchFilter casted) && isBucketRequired) {

            if(casted.getBucketSize() <= 0) throw new ValidationException("Field 'bucketSize' must be greater than 0", SOURCE_VALIDATE);

            final long numTimestamps = ((casted.getEndTimestamp() - casted.getStartTimestamp()) / casted.getBucketSize()) + 1;
            if(numTimestamps > MAX_TIMESTAMPS) throw new ValidationException("Too many timestamps: " + numTimestamps, SOURCE_VALIDATE);
        }
    }

    ///
}
