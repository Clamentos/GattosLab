package io.github.clamentos.gattoslab.observability;

///
import com.sun.net.httpserver.HttpExchange;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.FastAsciiJoiner;
import io.github.clamentos.gattoslab.datastructures.Siphon;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.observability.logging.LoggerRoot;
import io.github.clamentos.gattoslab.observability.metrics.SystemMetricsService;
import io.github.clamentos.gattoslab.observability.metrics.entities.CrawlAggregationEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.LineChartEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.LineChartEntry;
import io.github.clamentos.gattoslab.observability.metrics.entities.RequestMetricsAggregationEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.RequestMetricsEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.SystemMetricsAggregationEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.SystemMetricsEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.UserAgentAggregationEntity;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.Closeable;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

///
public final class ObservabilityService implements Closeable {

    ///
    private final Logger logger;

    ///..
    private final ObservabilityFile<RequestMetricsEntity> requestMetricsFile;
    private final ObservabilityFile<SystemMetricsEntity> systemMetricsFile;

    private final Siphon<RequestMetricsEntity> primarySiphon;
    private final Siphon<RequestMetricsEntity> secondarySiphon;
    private final AtomicReference<Siphon<RequestMetricsEntity>> currentSiphonReference;

    private final SystemMetricsService systemMetricsService;
    private final ObservabilityDatabase observabilityDatabase;

    ///
    public ObservabilityService(final BatchScheduler batchScheduler) throws IOException {

        this.logger = new Logger();

        this.requestMetricsFile = new ObservabilityFile<>(ApplicationProperties.REQUEST_METRICS_FILE_PATH, 13);
        this.systemMetricsFile = new ObservabilityFile<>(ApplicationProperties.SYSTEM_METRICS_FILE_PATH, 39);

        final Class<RequestMetricsEntity> type = RequestMetricsEntity.class;

        this.primarySiphon = new Siphon<>(type, ApplicationProperties.METRICS_SIPHON_CAPACITY, RequestMetricsEntity::new, this::drainSiphonTask);
        this.secondarySiphon = new Siphon<>(type, ApplicationProperties.METRICS_SIPHON_CAPACITY, RequestMetricsEntity::new, this::drainSiphonTask);
        this.currentSiphonReference = new AtomicReference<>(this.primarySiphon);

        this.systemMetricsService = new SystemMetricsService(batchScheduler.schedule(

            this::pollSystemMetricsTask,
            "gattos-lab-system-metrics-poll-task",
            ApplicationProperties.SYSTEM_METRICS_POLL_CRON
        ));

        this.observabilityDatabase = new ObservabilityDatabase(

            LoggerRoot.getInstance().getLogFile(),
            this.requestMetricsFile,
            this.systemMetricsFile
        );

        batchScheduler.schedule(

            () -> this.currentSiphonReference.get().drain(),
            "gattos-lab-metrics-drain-task",
            ApplicationProperties.METRICS_DRAIN_CRON
        );

        batchScheduler.schedule(

            this::retentionTask,
            "gattos-lab-observability-retention-task",
            ApplicationProperties.OBSERVABILITY_RETENTION_CRON
        );
    }

    ///
    public void requestStarted() {

        this.systemMetricsService.requestStarted();
    }

    ///..
    public void requestEnded(final HttpExchange exchange) {

        this.requestEndedInternal(exchange, (short)exchange.getResponseCode());
    }

    ///..
    public void requestPartiallyEnded(final HttpExchange exchange) {

        this.requestEndedInternal(exchange, (short)HttpStatus.TRUNCATED.getCode());
    }

    ///..
    public byte[] getLogs(final HttpExchange exchange) throws IOException, IllegalArgumentException {

        /*
            filter:
                startTime|endTime|severity|threadPattern|loggerPattern|messagePattern|exceptionClassPattern
                reqd     |reqd   |ok or ""|ok or ""     |ok or ""     |ok or ""      |ok or ""
        */

        final List<String> filter = this.validateAndExtractQuery(exchange, 7);

        final List<String> logs = this.observabilityDatabase.readLogs(

            Long.parseLong(filter.get(0)),
            Long.parseLong(filter.get(1)),
            filter.get(2),
            filter.get(3),
            filter.get(4),
            filter.get(5),
            filter.get(6)
        );

        final int length = logs.size();
        final FastAsciiJoiner joiner = new FastAsciiJoiner(length << 1);

        for(int i = 0; i < length; i++) {

            joiner.add(logs.get(i));
            joiner.add("\n");
        }

        joiner.deleteLast();
        return joiner.toByteArray();
    }

    ///..
    public byte[] getRequestMetrics(final HttpExchange exchange) throws IOException, IllegalArgumentException, NumberFormatException {

        /*
            filter:
                startTime|endTime|bucketSize
                reqd     |reqd   |reqd
        */

        final List<String> filter = this.validateAndExtractQuery(exchange, 3);

        final long startTimestamp = Long.parseLong(filter.get(0));
        final long endTimestamp = Long.parseLong(filter.get(1));
        final long bucketSize = Long.parseLong(filter.get(2));

        final List<String> requests = this.observabilityDatabase.readRequests(startTimestamp, endTimestamp, "", "");
        final int length = requests.size();

        // bucket -> status -> entity
        final Map<Long, Map<String, RequestMetricsAggregationEntity>> aggregationMap = new HashMap<>();
        final Set<String> allStatuses = new HashSet<>();

        for(int i = 0; i < length; i++) {

            final String request = requests.get(i);
            final List<String> splits = GenericUtils.fastSplit(request, ApplicationProperties.FIELD_SEPARATOR);
            final long bucket = (Long.parseLong(splits.get(1)) / bucketSize) * bucketSize;
            final String status = HttpStatus.decode(Integer.parseInt(splits.get(6))).toString();

            final RequestMetricsAggregationEntity aggregation = aggregationMap

                .computeIfAbsent(bucket, _ -> new HashMap<>())
                .computeIfAbsent(status, _ -> new RequestMetricsAggregationEntity())
            ;

            aggregation.setCount(aggregation.getCount() + 1);
            aggregation.setLatencySum(aggregation.getLatencySum() + Long.parseLong(splits.get(2)));
            allStatuses.add(status);
        }

        final long minNormalizedBucket = (startTimestamp / bucketSize) * bucketSize;
        final long maxNormalizedBucket = (endTimestamp / bucketSize) * bucketSize;
        final int timelineLength = (int)((maxNormalizedBucket - minNormalizedBucket) / bucketSize) + 1;

        if(timelineLength > ApplicationProperties.MAX_OBSERVABILITY_CHART_LENGTH) {

            throw new IllegalArgumentException("Chart too big, requested: " + timelineLength);
        }

        final long[] timeline = new long[timelineLength];
        long counter = minNormalizedBucket;

        for(int i = 0; i < timelineLength; i++) {

            timeline[i] = counter;
            counter += bucketSize;
        }

        final int allStatusesSize = allStatuses.size();
        final LineChartEntry[] rpsDatasets = new LineChartEntry[allStatusesSize];
        final LineChartEntry[] latencyDatasets = new LineChartEntry[allStatusesSize];
        int idx = 0;

        for(final String status : allStatuses) {

            rpsDatasets[idx] = new LineChartEntry(status, timelineLength);
            latencyDatasets[idx] = new LineChartEntry(status, timelineLength);

            idx++;
        }

        for(int i = 0; i < timelineLength; i++) {

            final Map<String, RequestMetricsAggregationEntity> innerMap = aggregationMap.get(timeline[i]);

            if(innerMap != null) {

                for(int j = 0; j < allStatusesSize; j++) {

                    final RequestMetricsAggregationEntity aggregation = innerMap.get(rpsDatasets[j].getLabel());

                    if(aggregation != null) {

                        final int aggregationCount = aggregation.getCount();

                        rpsDatasets[j].getData()[i] = aggregationCount;
                        latencyDatasets[j].getData()[i] = Math.ceilDiv(aggregation.getLatencySum(), aggregationCount);
                    }
                }
            }
        }

        final FastAsciiJoiner json = new FastAsciiJoiner((((allStatusesSize * 6) + 3) << 1) + 4);
        final LineChartEntity rpsChart = new LineChartEntity(timeline, rpsDatasets);
        final LineChartEntity latencyChart = new LineChartEntity(timeline, latencyDatasets);

        json.add("{\"rates\":");
        rpsChart.appendBytes(json);
        json.add(",");
        json.add("\"latencies\":");
        latencyChart.appendBytes(json);
        json.add("}");

        return json.toByteArray();
    }

    ///..
    public byte[] getSystemMetrics(final HttpExchange exchange) throws IOException, IllegalArgumentException, NumberFormatException {

        /*
            filter:
                startTime|endTime|bucketSize
                reqd     |reqd   |reqd
        */

        final List<String> filter = this.validateAndExtractQuery(exchange, 3);

        final long startTimestamp = Long.parseLong(filter.get(0));
        final long endTimestamp = Long.parseLong(filter.get(1));
        final long bucketSize = Long.parseLong(filter.get(2));

        final List<String> systemMetrics = this.observabilityDatabase.readSystemMetrics(startTimestamp, endTimestamp);
        final int length = systemMetrics.size();

        // bucket -> status -> entity
        final Map<Long, SystemMetricsAggregationEntity> aggregationMap = new HashMap<>();

        for(int i = 0; i < length; i++) {

            final String request = systemMetrics.get(i);
            final List<String> splits = GenericUtils.fastSplit(request, ApplicationProperties.FIELD_SEPARATOR);
            final long bucket = (Long.parseLong(splits.get(1)) / bucketSize) * bucketSize;

            final SystemMetricsAggregationEntity aggregation = aggregationMap.computeIfAbsent(bucket, _ -> new SystemMetricsAggregationEntity());

            aggregation.setPlatformThreads(aggregation.getPlatformThreads() + Long.parseLong(splits.get(2)));
            aggregation.setClassesLoaded(aggregation.getClassesLoaded() + Long.parseLong(splits.get(3)));
            aggregation.setFileReads(aggregation.getFileReads() + Long.parseLong(splits.get(4)));
            aggregation.setFileWrites(aggregation.getFileWrites() + Long.parseLong(splits.get(5)));
            aggregation.setSocketReads(aggregation.getSocketReads() + Long.parseLong(splits.get(6)));
            aggregation.setSocketWrites(aggregation.getSocketWrites() + Long.parseLong(splits.get(7)));
            aggregation.setGcCounts(aggregation.getGcCounts() + Long.parseLong(splits.get(8)));
            aggregation.setGcPause(aggregation.getGcPause() + Long.parseLong(splits.get(9)));
            aggregation.setCpuLoadJvmUser(aggregation.getCpuLoadJvmUser() + Long.parseLong(splits.get(10)));
            aggregation.setCpuLoadJvmSystem(aggregation.getCpuLoadJvmSystem() + Long.parseLong(splits.get(11)));
            aggregation.setCpuLoadMachineTotal(aggregation.getCpuLoadMachineTotal() + Long.parseLong(splits.get(12)));
            aggregation.setSystemMemoryUsed(aggregation.getSystemMemoryUsed() + Long.parseLong(splits.get(13)));
            aggregation.setMetaSpaceUsed(aggregation.getMetaSpaceUsed() + Long.parseLong(splits.get(14)));
            aggregation.setDirectBuffersUsed(aggregation.getDirectBuffersUsed() + Long.parseLong(splits.get(15)));
            aggregation.setDirectBuffersMemoryUsed(aggregation.getDirectBuffersMemoryUsed() + Long.parseLong(splits.get(16)));
            aggregation.setHeapUsed(aggregation.getHeapUsed() + Long.parseLong(splits.get(17)));
            aggregation.setStorageUsed(aggregation.getStorageUsed() + Long.parseLong(splits.get(18)));
            aggregation.setRequestMetricsEquilibrium(aggregation.getRequestMetricsEquilibrium() + Long.parseLong(splits.get(19)));
            aggregation.setCount(aggregation.getCount() + 1);
        }

        final long minNormalizedBucket = (startTimestamp / bucketSize) * bucketSize;
        final long maxNormalizedBucket = (endTimestamp / bucketSize) * bucketSize;
        final int timelineLength = (int)((maxNormalizedBucket - minNormalizedBucket) / bucketSize) + 1;

        if(timelineLength > ApplicationProperties.MAX_OBSERVABILITY_CHART_LENGTH) {

            throw new IllegalArgumentException("Chart too big, requested: " + timelineLength);
        }

        final long[] timeline = new long[timelineLength];
        long counter = minNormalizedBucket;

        for(int i = 0; i < timelineLength; i++) {

            timeline[i] = counter;
            counter += bucketSize;
        }

        final int numProbes = SystemMetricsAggregationEntity.probes.length;
        final LineChartEntry[] datasets = new LineChartEntry[numProbes];

        for(int i = 0; i < numProbes; i++) {

            datasets[i] = new LineChartEntry(SystemMetricsAggregationEntity.probes[i], timelineLength);
        }

        for(int i = 0; i < timelineLength; i++) {

            final SystemMetricsAggregationEntity aggregation = aggregationMap.get(timeline[i]);

            if(aggregation != null) {

                datasets[0].getData()[i] = Math.ceilDiv(aggregation.getPlatformThreads(), aggregation.getCount());
                datasets[1].getData()[i] = Math.ceilDiv(aggregation.getClassesLoaded(), aggregation.getCount());
                datasets[2].getData()[i] = aggregation.getFileReads();
                datasets[3].getData()[i] = aggregation.getFileWrites();
                datasets[4].getData()[i] = aggregation.getSocketReads();
                datasets[5].getData()[i] = aggregation.getSocketWrites();
                datasets[6].getData()[i] = aggregation.getGcCounts();
                datasets[7].getData()[i] = aggregation.getGcPause();
                datasets[8].getData()[i] = Math.ceilDiv(aggregation.getCpuLoadJvmUser(), aggregation.getCount());
                datasets[9].getData()[i] = Math.ceilDiv(aggregation.getCpuLoadJvmSystem(), aggregation.getCount());
                datasets[10].getData()[i] = Math.ceilDiv(aggregation.getCpuLoadMachineTotal(), aggregation.getCount());
                datasets[11].getData()[i] = Math.ceilDiv(aggregation.getSystemMemoryUsed(), aggregation.getCount());
                datasets[12].getData()[i] = Math.ceilDiv(aggregation.getMetaSpaceUsed(), aggregation.getCount());
                datasets[13].getData()[i] = Math.ceilDiv(aggregation.getDirectBuffersUsed(), aggregation.getCount());
                datasets[14].getData()[i] = Math.ceilDiv(aggregation.getDirectBuffersMemoryUsed(), aggregation.getCount());
                datasets[15].getData()[i] = Math.ceilDiv(aggregation.getHeapUsed(), aggregation.getCount());
                datasets[16].getData()[i] = Math.ceilDiv(aggregation.getStorageUsed(), aggregation.getCount());
                datasets[17].getData()[i] = aggregation.getRequestMetricsEquilibrium();
            }
        }

        final String[] chartNames = new String[]{

            "threads",
            "classes",
            "ioResources",
            "garbageCollection",
            "cpuUtilization",
            "memoryUtilization",
            "storageUtilization",
            "requestMetricsEquilibrium"
        };

        final int numCharts = chartNames.length;
        final FastAsciiJoiner json = new FastAsciiJoiner(165);
        final LineChartEntity[] charts = new LineChartEntity[numCharts];

        charts[0] = new LineChartEntity(timeline, new LineChartEntry[]{datasets[0]});
        charts[1] = new LineChartEntity(timeline, new LineChartEntry[]{datasets[1]});
        charts[2] = new LineChartEntity(timeline, new LineChartEntry[]{datasets[2], datasets[3], datasets[4], datasets[5]});
        charts[3] = new LineChartEntity(timeline, new LineChartEntry[]{datasets[6], datasets[7]});
        charts[4] = new LineChartEntity(timeline, new LineChartEntry[]{datasets[8], datasets[9], datasets[10]});
        charts[5] = new LineChartEntity(timeline, new LineChartEntry[]{datasets[11], datasets[12], datasets[13], datasets[14], datasets[15]});
        charts[6] = new LineChartEntity(timeline, new LineChartEntry[]{datasets[16]});
        charts[7] = new LineChartEntity(timeline, new LineChartEntry[]{datasets[17]});

        json.add("{");

        for(int i = 0; i < numCharts; i++) {

            json.add("\"");
            json.add(chartNames[i]);
            json.add("\":");
            charts[i].appendBytes(json);
            json.add(",");
        }

        json.replaceLast("}");
        return json.toByteArray();
    }

    ///..
    public byte[] getCrawlMetrics(final HttpExchange exchange) throws IOException, IllegalArgumentException, NumberFormatException {

        /*
            filter:
                startTime|endTime|isUnknown|userAgentPattern
                reqd     |reqd   | ok or ""| ok or ""
        */

        final List<String> filter = this.validateAndExtractQuery(exchange, 4);

        final List<String> requests = this.observabilityDatabase.readRequests(

            Long.parseLong(filter.get(0)),
            Long.parseLong(filter.get(1)),
            filter.get(2),
            filter.get(3)
        );

        final int length = requests.size();

        final Map<String, CrawlAggregationEntity> crawlAggregation = new HashMap<>();
        final Map<String, UserAgentAggregationEntity> userAgentAggregation = new HashMap<>();

        for(int i = 0; i < length; i++) {

            final List<String> splits = GenericUtils.fastSplit(requests.get(i), ApplicationProperties.FIELD_SEPARATOR);

            final String path = splits.get(3);
            final String userAgent = splits.get(4);
            final long timestamp = Long.parseLong(splits.get(1));

            final CrawlAggregationEntity crawl = crawlAggregation.computeIfAbsent(path, _ -> new CrawlAggregationEntity(

                path,
                Boolean.parseBoolean(splits.get(5))
            ));

            if(timestamp > crawl.getLastCalled()) crawl.setLastCalled(timestamp);
            crawl.setNumberOfCalls(crawl.getNumberOfCalls() + 1);
            crawl.getStatuses().add(splits.get(6));

            final UserAgentAggregationEntity agent = userAgentAggregation.computeIfAbsent(userAgent, _ -> new UserAgentAggregationEntity(userAgent));

            if(timestamp > agent.getLastSeen()) agent.setLastSeen(timestamp);
            agent.setNumberOfCalls(agent.getNumberOfCalls() + 1);
        }

        final FastAsciiJoiner joiner = new FastAsciiJoiner((crawlAggregation.size() * 36) + (userAgentAggregation.size() * 6) + 1);
        final List<CrawlAggregationEntity> sortedCrawls = new ArrayList<>(crawlAggregation.values());
        final List<UserAgentAggregationEntity> sortedUserAgents = new ArrayList<>(userAgentAggregation.values());

        sortedCrawls.sort((a, b) -> b.getNumberOfCalls() - a.getNumberOfCalls());
        sortedUserAgents.sort((a, b) -> b.getNumberOfCalls() - a.getNumberOfCalls());

        for(final CrawlAggregationEntity sortedCrawl : sortedCrawls) {

            sortedCrawl.appendBytes(joiner);

            if(sortedCrawl.getStatuses().isEmpty()) joiner.add("\n");
            else joiner.replaceLast("\n");
        }

        joiner.add("\n");

        for(final UserAgentAggregationEntity sortedUserAgent : sortedUserAgents) {

            sortedUserAgent.appendBytes(joiner);
            joiner.add("\n");
        }

        joiner.deleteLast();
        return joiner.toByteArray();
    }

    ///..
    @Override
    public void close() {

        this.logger.info("Begin shutdown...");

        this.currentSiphonReference.get().drain();
        this.systemMetricsService.close();

        this.logger.info("End shutdown");
    }

    ///.
    private void requestEndedInternal(final HttpExchange exchange, final short statusCode) {

        final Boolean isTracked = (Boolean)exchange.getAttribute(ApplicationProperties.REQUEST_TRACKED_ATTRIBUTE);
        if(isTracked == Boolean.TRUE) return;

        while(!(this.currentSiphonReference.get().update(entity -> this.updateRequestMetrics(entity, exchange, statusCode)))) {

            GenericUtils.silentSleep(1);
        }

        this.systemMetricsService.requestMetricCreated();
        exchange.setAttribute(ApplicationProperties.REQUEST_TRACKED_ATTRIBUTE, true);
    }

    ///..
    private void updateRequestMetrics(final RequestMetricsEntity requestMetricsEntity, final HttpExchange exchange, final short statusCode) {

        final long startTime = (long)exchange.getAttribute(ApplicationProperties.REQUEST_START_TIME_ATTRIBUTE);

        requestMetricsEntity.setId((long)exchange.getAttribute(ApplicationProperties.REQUEST_REQUEST_ID_ATTRIBUTE));
        requestMetricsEntity.setTimestamp(startTime);
        requestMetricsEntity.setLatency((int)(System.currentTimeMillis() - startTime));
        requestMetricsEntity.setPath(exchange.getRequestURI().getPath());
        requestMetricsEntity.setUserAgent((String)exchange.getAttribute(ApplicationProperties.REQUEST_USER_AGENT_ATTRIBUTE));
        requestMetricsEntity.setUnknown(exchange.getAttribute(ApplicationProperties.REQUEST_RESOURCE_ATTRIBUTE) == null);
        requestMetricsEntity.setHttpStatus(statusCode);
    }

    ///..
    private List<String> validateAndExtractQuery(final HttpExchange exchange, final int numComponents) throws IllegalArgumentException {

        final String query = GenericUtils.extractQueryParam(exchange.getRequestURI().getQuery(), "filter");
        if(query == null) throw new IllegalArgumentException("Filter must be provided");

        final List<String> filter = GenericUtils.fastSplit(query, ApplicationProperties.FIELD_SEPARATOR);

        if(filter.size() != numComponents) {

            throw new IllegalArgumentException("Filter must have " + numComponents + " components, got '" + filter + "'");
        }

        return filter;
    }

    ///..
    private void drainSiphonTask(final List<RequestMetricsEntity> requestMetrics) {

        if(!requestMetrics.isEmpty()) {

            final Siphon<RequestMetricsEntity> previousSiphon;
            if(this.currentSiphonReference.compareAndSet(this.primarySiphon, this.secondarySiphon)) previousSiphon = this.primarySiphon;

            else {

                this.currentSiphonReference.set(this.primarySiphon);
                previousSiphon = this.secondarySiphon;
            }

            while(previousSiphon.isBusy()) {

                GenericUtils.silentSleep(1);
            }

            try {

                this.requestMetricsFile.write(requestMetrics);
            }

            catch(final IOException exc) {

                this.logger.error("Could not write to request metrics file because", exc);
            }
        }
    }

    ///..
    private void pollSystemMetricsTask() {

        if(this.systemMetricsService != null) {

            final SystemMetricsEntity systemMetric = this.systemMetricsService.sample();

            if(systemMetric != null) {

                try {

                    this.systemMetricsFile.write(List.of(systemMetric));
                }

                catch(final IOException exc) {

                    this.logger.error("Could not write to system metrics file because", exc);
                }
            }
        }
    }

    ///..
    private void retentionTask() {

        final long days = ApplicationProperties.OBSERVABILITY_DATA_RETENTION.toDays();
        int numDeleted = 0;

        numDeleted += this.applyRetention(LoggerRoot.getInstance().getLogFile(), days);
        numDeleted += this.applyRetention(requestMetricsFile, days);
        numDeleted += this.applyRetention(systemMetricsFile, days);

        if(numDeleted > 0) this.logger.info("Deleted " + numDeleted + " observability files");
    }

    ///..
    private int applyRetention(final ObservabilityFile<?> observabilityFile, final long days) {

        try {

            return observabilityFile.deleteAll(LocalDateTime.now(GenericUtils.DEFAULT_ZONE_ID).minusDays(days).truncatedTo(ChronoUnit.HOURS));
        }

        catch(final IOException exc) {

            this.logger.error("Could not delete old files from '" + observabilityFile.toString() + "', because", exc);
            return 0;
        }
    }

    ///
}
