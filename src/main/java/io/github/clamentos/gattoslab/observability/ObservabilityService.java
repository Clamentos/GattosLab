package io.github.clamentos.gattoslab.observability;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.eventbus.EventBus;
import io.github.clamentos.gattoslab.exceptions.ValidationException;
import io.github.clamentos.gattoslab.http.HttpUtils;
import io.github.clamentos.gattoslab.http.ResponseSender;
import io.github.clamentos.gattoslab.observability.filters.AggregatedSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.RequestMetricsSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.SearchFilter;
import io.github.clamentos.gattoslab.observability.metrics.ObservabilityContext;
import io.github.clamentos.gattoslab.observability.metrics.SystemMetrics;
import io.github.clamentos.gattoslab.observability.metrics.entities.PathInvocationAggregationEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.RequestMetricsAggregateEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.RequestMetricsEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.SystemMetricsEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.UserAgentAggregationEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.charts.ChartDataset;
import io.github.clamentos.gattoslab.observability.metrics.entities.charts.RequestMetricsCharts;
import io.github.clamentos.gattoslab.observability.metrics.entities.charts.SystemMetricsCharts;
import io.github.clamentos.gattoslab.observability.metrics.entities.charts.LineChart;
import io.github.clamentos.gattoslab.persistence.EntityType;
import io.github.clamentos.gattoslab.persistence.FileDatabase;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

///..
import lombok.extern.slf4j.Slf4j;

///..
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

///..
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;

///
@Slf4j()

///
public class ObservabilityService implements Closeable {

    ///
    private static final int MAX_TIMESTAMPS = 10000;
    private static final String SOURCE_VALIDATE = "ObservabilityService.validateSearchFilter";
    private static final Logger REQUEST_METRICS_LOGGER = LoggerFactory.getLogger("REQUEST_METRICS_LOGGER");
    private static final Logger SYSTEM_METRICS_LOGGER = LoggerFactory.getLogger("SYSTEM_METRICS_LOGGER");

    ///..
    private final int siphonCapacity;
    private final Set<String> monitoredPaths;

    ///..
    private final FileDatabase fileDatabase;

    ///..
    private final SystemMetrics systemMetrics;
    private final EventBus eventBus;
    private final AtomicBoolean isHandlingEvent;

    private final AtomicReference<ObservabilityContext> primaryContext;
    private final AtomicReference<ObservabilityContext> secondaryContext;

    ///
    public ObservabilityService(

        final ApplicationProperties applicationProperties,
        final BatchScheduler batchScheduler,
        final Website website,
        final FileDatabase fileDatabase

    ) throws IllegalArgumentException {

        eventBus = new EventBus(this::dumpMetrics);
        systemMetrics = new SystemMetrics(SimpleCron.decodePeriod(applicationProperties.getSystemMetricsSampling()));

        batchScheduler.schedule(this::sampleSystemMetrics, "ObservabilityService::sampleSystemMetrics", applicationProperties.getSystemMetricsPolling());
        batchScheduler.schedule(eventBus::trigger, "ObservabilityService::trigger", applicationProperties.getMetricsDumpToDbSchedule());

        siphonCapacity = applicationProperties.getMetricsSiphonCapacity();
        monitoredPaths = website.getPaths();

        this.fileDatabase = fileDatabase;

        primaryContext = new AtomicReference<>(new ObservabilityContext(eventBus, siphonCapacity));
        secondaryContext = new AtomicReference<>(new ObservabilityContext(eventBus, siphonCapacity));

        isHandlingEvent = new AtomicBoolean();
    }

    ///
    public ResponseSender getRequestMetrics(final JsonGenerator generator, final RequestMetricsSearchFilter searchFilter)
    throws IOException, JacksonException, ValidationException {

        this.validateSearchFilter(searchFilter, true);

        final long bucketSize = searchFilter.getBucketSize();
        final long[] labels = new long[(int)((searchFilter.getEndTimestamp() - searchFilter.getStartTimestamp()) / bucketSize) + 1];
        for(int i = 0; i < labels.length; i++) labels[i] = (i * bucketSize) + searchFilter.getStartTimestamp();

        final List<RequestMetricsEntity> entities = fileDatabase.fetchByFilter(EntityType.REQUEST_METRICS, searchFilter, RequestMetricsEntity.class);
        final Map<String, Map<Long, RequestMetricsAggregateEntity>> metricsAggregationMap = new HashMap<>();

        for(final RequestMetricsEntity entity : entities) {

            final int bucketIndex = (int)((entity.getTimestamp() - searchFilter.getStartTimestamp()) / bucketSize);
            final String key = Integer.toString(entity.getHttpStatus()) + (entity.isOthers() ? "<others>" : entity.getPath());

            metricsAggregationMap.computeIfAbsent(key, _ -> new TreeMap<>()).computeIfAbsent(labels[bucketIndex], _ -> new RequestMetricsAggregateEntity()).update(entity);
            metricsAggregationMap.computeIfAbsent("TOTAL", _ -> new TreeMap<>()).computeIfAbsent(labels[bucketIndex], _ -> new RequestMetricsAggregateEntity()).update(entity);
        }

        for(final Map<Long, RequestMetricsAggregateEntity> innerMetricsAggregationMap : metricsAggregationMap.values()) {

            for(int i = 0; i < labels.length; i++) {

                innerMetricsAggregationMap.putIfAbsent(labels[i], null);
            }
        }

        final List<ChartDataset<long[]>> rateDatasets = new ArrayList<>();
        final List<ChartDataset<long[]>> latencyDatasets = new ArrayList<>();

        for(final Map.Entry<String, Map<Long, RequestMetricsAggregateEntity>> metricsMapEntry : metricsAggregationMap.entrySet()) {

            final Collection<RequestMetricsAggregateEntity> innerEntities = metricsMapEntry.getValue().values();
            final long[] rateData = new long[innerEntities.size()];
            final long[] latencyData = new long[innerEntities.size()];

            int index = 0;

            for(final Map.Entry<Long, RequestMetricsAggregateEntity> entry : metricsMapEntry.getValue().entrySet()) {

                final RequestMetricsAggregateEntity entity = entry.getValue();

                if(entity != null) {

                    rateData[index] = entity.getRate();
                    latencyData[index] = entity.getLatencySum() / entity.getRate();
                }

                else {

                    rateData[index] = 0;
                    latencyData[index] = 0;
                }

                index++;
            }

            final String key = metricsMapEntry.getKey();

            rateDatasets.add(new ChartDataset<>(key, rateData));
            latencyDatasets.add(new ChartDataset<>(key, latencyData));
        }

        return () -> generator.writePOJO(new RequestMetricsCharts(new LineChart(labels, rateDatasets), new LineChart(labels, latencyDatasets)));
    }

    ///..
    public ResponseSender getInvocationMetrics(final JsonGenerator generator, final RequestMetricsSearchFilter searchFilter)
    throws IOException, JacksonException, ValidationException {

        this.validateSearchFilter(searchFilter, false);

        final List<RequestMetricsEntity> entities = fileDatabase.fetchByFilter(EntityType.REQUEST_METRICS, searchFilter, RequestMetricsEntity.class);
        final Map<String, List<RequestMetricsEntity>> pathsAggregationMap = new HashMap<>();
        final Map<String, List<RequestMetricsEntity>> userAgentsAggregationMap = new HashMap<>();

        for(final RequestMetricsEntity entity : entities) {

            pathsAggregationMap.computeIfAbsent(entity.getPath(), _ -> new ArrayList<>()).add(entity);
            userAgentsAggregationMap.computeIfAbsent(entity.getUserAgent(), _ -> new ArrayList<>()).add(entity);
        }

        final List<PathInvocationAggregationEntity> pathAggregates = new ArrayList<>();
        final List<UserAgentAggregationEntity> userAgentAggregates = new ArrayList<>();

        for(final Map.Entry<String, List<RequestMetricsEntity>> entry : pathsAggregationMap.entrySet()) {

            long firstInvocation = Long.MAX_VALUE;
            long lastInvocation = Long.MIN_VALUE;
            Set<Integer> httpStatuses = new HashSet<>();
            int count = 0;

            for(final RequestMetricsEntity entity : entry.getValue()) {

                firstInvocation = Math.min(firstInvocation, entity.getTimestamp());
                lastInvocation = Math.max(lastInvocation, entity.getTimestamp());
                httpStatuses.add(entity.getHttpStatus());
                count++;
            }

            int idx = 0;
            final int[] httpStatusesArr = new int[httpStatuses.size()];
            for(final Integer httpStatus : httpStatuses) httpStatusesArr[idx++] = httpStatus;
            pathAggregates.add(new PathInvocationAggregationEntity(entry.getKey(), firstInvocation, lastInvocation, count, entry.getValue().get(0).isOthers(), httpStatusesArr));
        }

        for(final Map.Entry<String, List<RequestMetricsEntity>> entry : userAgentsAggregationMap.entrySet()) {

            long firstInvocation = Long.MAX_VALUE;
            long lastInvocation = Long.MIN_VALUE;
            int count = 0;

            for(final RequestMetricsEntity entity : entry.getValue()) {

                firstInvocation = Math.min(firstInvocation, entity.getTimestamp());
                lastInvocation = Math.max(lastInvocation, entity.getTimestamp());
                count++;
            }

            userAgentAggregates.add(new UserAgentAggregationEntity(entry.getKey(), firstInvocation, lastInvocation, count));
        }

        pathAggregates.sort((a, b) -> b.getCount() - a.getCount());
        userAgentAggregates.sort((a, b) -> b.getCount() - a.getCount());

        return () -> {

            generator.writeStartObject();

            generator.writeArrayPropertyStart("paths");
            for(final PathInvocationAggregationEntity pathAggregate : pathAggregates) generator.writePOJO(pathAggregate);
            generator.writeEndArray();

            generator.writeArrayPropertyStart("userAgents");
            for(final UserAgentAggregationEntity userAgentAggregate : userAgentAggregates) generator.writePOJO(userAgentAggregate);
            generator.writeEndArray();

            generator.writeEndObject();
        };
    }

    ///..
    public ResponseSender getSystemMetrics(final JsonGenerator generator, final AggregatedSearchFilter searchFilter) throws IOException, JacksonException, ValidationException {

        this.validateSearchFilter(searchFilter, true);

        final long bucketSize = searchFilter.getBucketSize();
        final long[] labels = new long[(int)((searchFilter.getEndTimestamp() - searchFilter.getStartTimestamp()) / bucketSize) + 1];
        final Map<Long, List<SystemMetricsEntity>> metricsAggregationMap = new TreeMap<>();

        for(int i = 0; i < labels.length; i++) {

            labels[i] = (i * bucketSize) + searchFilter.getStartTimestamp();
            metricsAggregationMap.putIfAbsent(labels[i], null);
        }

        final List<SystemMetricsEntity> entities = fileDatabase.fetchByFilter(EntityType.SYSTEM_METRICS, searchFilter, SystemMetricsEntity.class);

        for(final SystemMetricsEntity entity : entities) {

            final int bucketIndex = (int)((entity.getTimestamp() - searchFilter.getStartTimestamp()) / bucketSize);
            metricsAggregationMap.computeIfAbsent(labels[bucketIndex], _ -> new ArrayList<>()).add(entity);
        }

        final int actualSize = metricsAggregationMap.size();

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

        for(final Map.Entry<Long, List<SystemMetricsEntity>> entry : metricsAggregationMap.entrySet()) {

            if(entry.getValue() != null) {

                final int count = entry.getValue().size();

                long virtualThreadsTmp = 0;
                long platformThreadsTmp = 0;
                long classesTmp = 0;
                long fileReadsTmp = 0;
                long fileWritesTmp = 0;
                long socketReadsTmp = 0;
                long socketWritesTmp = 0;
                long gcCountsTmp = 0;
                long gcPauseTmp = 0;
                long cpuUserTmp = 0;
                long cpuSystemTmp = 0;
                long cpuMachineTmp = 0;
                long systemMemoryUsedTmp = 0;
                long metaSpaceUsedTmp = 0;
                long directBuffersUsedTmp = 0;
                long directBuffersMemoryUsedTmp = 0;
                long heapUsedTmp = 0;
                long storageUsedTmp = 0;

                for(final SystemMetricsEntity entity : entry.getValue()) {

                    virtualThreadsTmp = virtualThreadsTmp + entity.getVirtualThreads();
                    platformThreadsTmp = platformThreadsTmp + entity.getPlatformThreads();
                    classesTmp = classesTmp + entity.getClassesLoaded();
                    fileReadsTmp = fileReadsTmp + entity.getFileReads();
                    fileWritesTmp = fileWritesTmp + entity.getFileWrites();
                    socketReadsTmp = socketReadsTmp + entity.getSocketReads();
                    socketWritesTmp = socketWritesTmp + entity.getSocketWrites();
                    gcCountsTmp = gcCountsTmp + entity.getGcCounts();
                    gcPauseTmp = gcPauseTmp + entity.getGcPause();
                    cpuUserTmp = cpuUserTmp + entity.getCpuLoadJvmUser();
                    cpuSystemTmp = cpuSystemTmp + entity.getCpuLoadJvmSystem();
                    cpuMachineTmp = cpuMachineTmp + entity.getCpuLoadMachineTotal();
                    systemMemoryUsedTmp = systemMemoryUsedTmp + entity.getSystemMemoryUsed();
                    metaSpaceUsedTmp = metaSpaceUsedTmp + entity.getMetaSpaceUsed();
                    directBuffersUsedTmp = directBuffersUsedTmp + entity.getDirectBuffersUsed();
                    directBuffersMemoryUsedTmp = directBuffersMemoryUsedTmp + entity.getDirectBuffersMemoryUsed();
                    heapUsedTmp = heapUsedTmp + entity.getHeapUsed();
                    storageUsedTmp = storageUsedTmp + entity.getStorageUsed();
                }

                virtualThreads[index] = Math.ceilDiv(virtualThreadsTmp, count);
                platformThreads[index] = Math.ceilDiv(platformThreadsTmp, count);
                classes[index] = Math.ceilDiv(classesTmp, count);
                fileReads[index] = fileReadsTmp;
                fileWrites[index] = fileWritesTmp;
                socketReads[index] = socketReadsTmp;
                socketWrites[index] = socketWritesTmp;
                gcCounts[index] = gcCountsTmp;
                gcPause[index] = gcPauseTmp;
                cpuUser[index] = Math.ceilDiv(cpuUserTmp, count);
                cpuSystem[index] = Math.ceilDiv(cpuSystemTmp, count);
                cpuMachine[index] = Math.ceilDiv(cpuMachineTmp, count);
                systemMemoryUsed[index] = Math.ceilDiv(systemMemoryUsedTmp, count);
                metaSpaceUsed[index] = Math.ceilDiv(metaSpaceUsedTmp, count);
                directBuffersUsed[index] = Math.ceilDiv(directBuffersUsedTmp, count);
                directBuffersMemoryUsed[index] = Math.ceilDiv(directBuffersMemoryUsedTmp, count);
                heapUsed[index] = Math.ceilDiv(heapUsedTmp, count);
                storageUsed[index] = Math.ceilDiv(storageUsedTmp, count);
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

        final SystemMetricsEntity entity = systemMetrics.toEntity();
        if(entity != null) SYSTEM_METRICS_LOGGER.trace("{}", entity);
    }

    ///..
    private void dumpMetrics() {

        if(isHandlingEvent.compareAndSet(false, true)) {

            this.insertMetrics(this.swapContexts());
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
    private void insertMetrics(final ObservabilityContext context) {

        while(!context.isNoOneThere()) GenericUtils.silentSleep(1L);
        for(final RequestMetricsEntity entity : context.drainSiphon()) REQUEST_METRICS_LOGGER.trace("{}", entity);
        context.reset();
    }

    ///..
    private void validateSearchFilter(final SearchFilter temporalSearchFilter, final boolean isBucketRequired) throws ValidationException {

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
