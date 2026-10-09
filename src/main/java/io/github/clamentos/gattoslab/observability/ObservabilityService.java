package io.github.clamentos.gattoslab.observability;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.Siphon;
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.server.HttpExchange;
import io.github.clamentos.gattoslab.http.server.ResponseBodyCallback;
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
import java.time.Duration;
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
    private final ObservabilityDirectory<RequestMetricsEntity> requestMetricsDirectory;
    private final ObservabilityDirectory<SystemMetricsEntity> systemMetricsDirectory;

    private final Siphon<RequestMetricsEntity> primarySiphon;
    private final Siphon<RequestMetricsEntity> secondarySiphon;
    private final AtomicReference<Siphon<RequestMetricsEntity>> currentSiphonReference;

    private final SystemMetricsService systemMetricsService;
    private final ObservabilityDatabase observabilityDatabase;

    ///
    public ObservabilityService(final ApplicationProperties applicationProperties, final BatchScheduler batchScheduler) throws IOException {

        this.logger = new Logger();
        final Logger fileLogger = new Logger(ObservabilityDirectory.class.getSimpleName());

        this.requestMetricsDirectory = new ObservabilityDirectory<>(fileLogger, ApplicationProperties.REQUEST_METRICS_FILE_PATH);
        this.systemMetricsDirectory = new ObservabilityDirectory<>(fileLogger, ApplicationProperties.SYSTEM_METRICS_FILE_PATH);

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

            LoggerRoot.getInstance().getLogDirectory(),
            this.requestMetricsDirectory,
            this.systemMetricsDirectory
        );

        batchScheduler.schedule(

            () -> this.currentSiphonReference.get().drain(),
            "gattos-lab-metrics-drain-task",
            ApplicationProperties.METRICS_DRAIN_CRON
        );

        batchScheduler.schedule(

            () -> this.retentionTask(applicationProperties.getObservabilityDataRetention()),
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

        this.requestEndedInternal(exchange, exchange.getResponseStatus());
    }

    ///..
    public void requestPartiallyEnded(final HttpExchange exchange) {

        this.requestEndedInternal(exchange, HttpStatus.TRUNCATED);
    }

    ///..
    public ResponseBodyCallback getLogs(final HttpExchange exchange) throws IOException, IllegalArgumentException {

        /*
            filter:
                startTime|endTime|severity|threadPattern|loggerPattern|messagePattern|exceptionClassPattern
                reqd     |reqd   |ok or ""|ok or ""     |ok or ""     |ok or ""      |ok or ""
        */

        final List<String> filter = this.validateAndExtractFilter(exchange, 7);

        final List<String> logs = this.observabilityDatabase.readLogs(

            Long.parseLong(filter.get(0)),
            Long.parseLong(filter.get(1)),
            filter.get(2),
            filter.get(3),
            filter.get(4),
            filter.get(5),
            filter.get(6)
        );

        return writer -> {

            final int length = logs.size();

            for(int i = 0; i < length; i++) {

                writer.write(logs.get(i));
                writer.write('\n');
            }
        };
    }

    ///..
    public ResponseBodyCallback getRequestMetrics(final HttpExchange exchange) throws IOException, IllegalArgumentException {

        /*
            filter:
                startTime|endTime|bucketSize
                reqd     |reqd   |reqd
        */

        final List<String> filter = this.validateAndExtractFilter(exchange, 3);

        final long startTimestamp = Long.parseLong(filter.get(0));
        final long endTimestamp = Long.parseLong(filter.get(1));
        final long bucketSize = Long.parseLong(filter.get(2));

        if(bucketSize <= 0) throw new IllegalArgumentException("Filter field 'bucketSize' must be > 0");

        final List<String> requests = this.observabilityDatabase.readRequests(startTimestamp, endTimestamp, "", "", "");
        final int length = requests.size();

        final Map<Long, Map<String, RequestMetricsAggregationEntity>> aggregationMap = new HashMap<>();
        final Set<String> allStatuses = new HashSet<>();

        for(int i = 0; i < length; i++) {

            final List<String> splits = GenericUtils.fastSplit(requests.get(i), ApplicationProperties.FIELD_SEPARATOR);
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

        final LineChartEntity rpsChart = new LineChartEntity(timeline, rpsDatasets);
        final LineChartEntity latencyChart = new LineChartEntity(timeline, latencyDatasets);

        return writer -> {

            writer.write("{\"rates\":");
            rpsChart.stream(writer);
            writer.write(",\"latencies\":");
            latencyChart.stream(writer);
            writer.write("}");
        };
    }

    ///..
    public ResponseBodyCallback getSystemMetrics(final HttpExchange exchange) throws IOException, IllegalArgumentException {

        /*
            filter:
                startTime|endTime|bucketSize
                reqd     |reqd   |reqd
        */

        final List<String> filter = this.validateAndExtractFilter(exchange, 3);

        final long startTimestamp = Long.parseLong(filter.get(0));
        final long endTimestamp = Long.parseLong(filter.get(1));
        final long bucketSize = Long.parseLong(filter.get(2));

        if(bucketSize <= 0) throw new IllegalArgumentException("Filter field 'bucketSize' must be > 0");

        final List<String> systemMetrics = this.observabilityDatabase.readSystemMetrics(startTimestamp, endTimestamp);
        final int length = systemMetrics.size();

        final Map<Long, SystemMetricsAggregationEntity> aggregationMap = new HashMap<>();

        for(int i = 0; i < length; i++) {

            final List<String> splits = GenericUtils.fastSplit(systemMetrics.get(i), ApplicationProperties.FIELD_SEPARATOR);
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
            aggregation.setHeapUsed(aggregation.getHeapUsed() + Long.parseLong(splits.get(15)));
            aggregation.setStorageUsed(aggregation.getStorageUsed() + Long.parseLong(splits.get(16)));
            aggregation.setRequestMetricsEquilibrium(aggregation.getRequestMetricsEquilibrium() + Long.parseLong(splits.get(17)));
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

        final int numProbes = SystemMetricsAggregationEntity.getProbeNames().length;
        final LineChartEntry[] datasets = new LineChartEntry[numProbes];

        for(int i = 0; i < numProbes; i++) {

            datasets[i] = new LineChartEntry(SystemMetricsAggregationEntity.getProbeNames()[i], timelineLength);
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
                datasets[13].getData()[i] = Math.ceilDiv(aggregation.getHeapUsed(), aggregation.getCount());
                datasets[14].getData()[i] = Math.ceilDiv(aggregation.getStorageUsed(), aggregation.getCount());
                datasets[15].getData()[i] = aggregation.getRequestMetricsEquilibrium();
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

        final LineChartEntity[] charts = new LineChartEntity[chartNames.length];

        charts[0] = new LineChartEntity(timeline, new LineChartEntry[]{datasets[0]});
        charts[1] = new LineChartEntity(timeline, new LineChartEntry[]{datasets[1]});
        charts[2] = new LineChartEntity(timeline, new LineChartEntry[]{datasets[2], datasets[3], datasets[4], datasets[5]});
        charts[3] = new LineChartEntity(timeline, new LineChartEntry[]{datasets[6], datasets[7]});
        charts[4] = new LineChartEntity(timeline, new LineChartEntry[]{datasets[8], datasets[9], datasets[10]});
        charts[5] = new LineChartEntity(timeline, new LineChartEntry[]{datasets[11], datasets[12], datasets[13]});
        charts[6] = new LineChartEntity(timeline, new LineChartEntry[]{datasets[14]});
        charts[7] = new LineChartEntity(timeline, new LineChartEntry[]{datasets[15]});

        return writer -> {

            final int limit = chartNames.length - 1;
            writer.write("{");

            for(int i = 0; i < limit; i++) {

                writer.write("\"");
                writer.write(chartNames[i]);
                writer.write("\":");
                charts[i].stream(writer);
                writer.write(",");
            }

            writer.write("\"");
            writer.write(chartNames[limit]);
            writer.write("\":");
            charts[limit].stream(writer);
            writer.write("}");
        };
    }

    ///..
    public ResponseBodyCallback getCrawlMetrics(final HttpExchange exchange) throws IOException, IllegalArgumentException {

        /*
            filter:
                startTime|endTime|isUnknown|isUserAgentBlocked|userAgentPattern
                reqd     |reqd   | ok or ""| ok or ""         | ok or ""
        */

        final List<String> filter = this.validateAndExtractFilter(exchange, 5);

        final List<String> requests = this.observabilityDatabase.readRequests(

            Long.parseLong(filter.get(0)),
            Long.parseLong(filter.get(1)),
            filter.get(2),
            filter.get(3),
            filter.get(4)
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

            final UserAgentAggregationEntity agent = userAgentAggregation.computeIfAbsent(userAgent, _ -> new UserAgentAggregationEntity(

                userAgent,
                Boolean.parseBoolean(splits.get(7))
            ));

            if(timestamp > agent.getLastSeen()) agent.setLastSeen(timestamp);
            agent.setNumberOfCalls(agent.getNumberOfCalls() + 1);
        }

        final List<CrawlAggregationEntity> sortedCrawls = new ArrayList<>(crawlAggregation.values());
        final List<UserAgentAggregationEntity> sortedUserAgents = new ArrayList<>(userAgentAggregation.values());

        sortedCrawls.sort((a, b) -> b.getNumberOfCalls() - a.getNumberOfCalls());
        sortedUserAgents.sort((a, b) -> b.getNumberOfCalls() - a.getNumberOfCalls());

        return writer -> {

            final int sortedCrawlsLength = sortedCrawls.size();
            final int sortedUserAgentsLength = sortedUserAgents.size();

            for(int i = 0; i < sortedCrawlsLength; i++) {

                sortedCrawls.get(i).stream(writer);
                writer.write('\n');
            }

            writer.write('\n');

            for(int i = 0; i < sortedUserAgentsLength; i++) {

                sortedUserAgents.get(i).stream(writer);
                writer.write('\n');
            }
        };
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
    private void requestEndedInternal(final HttpExchange exchange, final HttpStatus status) {

        if(exchange.isTracked()) return;

        while(!(this.currentSiphonReference.get().update(entity -> this.updateRequestMetrics(entity, exchange, status)))) {

            GenericUtils.silentSleepNanos(100_000);
        }

        this.systemMetricsService.requestMetricCreated();
        exchange.setTracked(true);
    }

    ///..
    private void updateRequestMetrics(final RequestMetricsEntity requestMetricsEntity, final HttpExchange exchange, final HttpStatus status) {

        final long startTime = exchange.getStartTime();

        requestMetricsEntity.setId(exchange.getRequestId());
        requestMetricsEntity.setTimestamp(startTime);
        requestMetricsEntity.setLatency((int)(System.currentTimeMillis() - startTime));
        requestMetricsEntity.setUri(exchange.getUri());
        requestMetricsEntity.setUserAgent(exchange.getRequestHeaders().get(HttpHeader.USER_AGENT));
        requestMetricsEntity.setUnknown(exchange.getResource() == null);
        requestMetricsEntity.setHttpStatus((short)status.getCode());
        requestMetricsEntity.setUserAgentBlocked(exchange.isUserAgentBlocked());
    }

    ///..
    private List<String> validateAndExtractFilter(final HttpExchange exchange, final int numComponents) throws IllegalArgumentException {

        final List<String> splits = GenericUtils.fastSplit(

            exchange.getRequestHeaders().get(HttpHeader.FILTER),
            ApplicationProperties.FIELD_SEPARATOR
        );

        if(splits.isEmpty()) throw new IllegalArgumentException("Header 'Filter' must be provided");
        if(splits.size() != numComponents) throw new IllegalArgumentException("'Filter' must have " + numComponents + " components");

        return splits;
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

            while(previousSiphon.isUpdating()) {

                GenericUtils.silentSleepNanos(100_000);
            }

            try {

                this.requestMetricsDirectory.write(requestMetrics, false);
            }

            catch(final IOException exc) {

                this.logger.error("Could not write to request metrics file", exc);
            }
        }
    }

    ///..
    private void pollSystemMetricsTask() {

        if(this.systemMetricsService != null) {

            final SystemMetricsEntity systemMetric = this.systemMetricsService.sample();

            if(systemMetric != null) {

                try {

                    this.systemMetricsDirectory.write(List.of(systemMetric), false);
                }

                catch(final IOException exc) {

                    this.logger.error("Could not write to system metrics file", exc);
                }
            }
        }
    }

    ///..
    private void retentionTask(final Duration retention) {

        final long days = retention.toDays();
        final LocalDateTime boundaryDate = LocalDateTime.now(GenericUtils.DEFAULT_ZONE_ID).minusDays(days).truncatedTo(ChronoUnit.HOURS);

        final int numLogFilesDeleted = this.applyRetention(LoggerRoot.getInstance().getLogDirectory(), boundaryDate);
        final int numRequestMetricFilesDeleted = this.applyRetention(requestMetricsDirectory, boundaryDate);
        final int numSystemMetricFilesDeleted = this.applyRetention(systemMetricsDirectory, boundaryDate);

        if(numLogFilesDeleted > 0 || numRequestMetricFilesDeleted > 0 || numSystemMetricFilesDeleted > 0) {

            this.logger.info(

                "Deleted " + numLogFilesDeleted + " log files, " +
                numRequestMetricFilesDeleted + " request metric files, " +
                numSystemMetricFilesDeleted + " system metric files"
            );
        }
    }

    ///..
    private int applyRetention(final ObservabilityDirectory<?> observabilityFile, final LocalDateTime boundaryDate) {

        try {

            return observabilityFile.deleteAll(boundaryDate);
        }

        catch(final IOException exc) {

            this.logger.error("Could not delete old files from '" + observabilityFile.toString() + "'", exc);
            return 0;
        }
    }

    ///
}
