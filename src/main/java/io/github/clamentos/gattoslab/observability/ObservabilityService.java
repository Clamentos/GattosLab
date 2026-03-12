package io.github.clamentos.gattoslab.observability;

///
import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.pojos.MetricsConfig;
import io.github.clamentos.gattoslab.eventbus.EventBus;
import io.github.clamentos.gattoslab.observability.filters.AggregationPipelines;
import io.github.clamentos.gattoslab.observability.filters.RequestMetricsSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.TemporalSearchFilter;
import io.github.clamentos.gattoslab.observability.metrics.ObservabilityContext;
import io.github.clamentos.gattoslab.observability.metrics.SystemMetrics;
import io.github.clamentos.gattoslab.observability.metrics.entities.PathInvocationAggregationEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.RequestMetricsAggregateEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.RequestMetricsEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.SystemMetricsEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.UserAgentAggregationEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.charts.BubbleChartDataEntry;
import io.github.clamentos.gattoslab.observability.metrics.entities.charts.ChartDataset;
import io.github.clamentos.gattoslab.observability.metrics.entities.charts.RequestMetricsCharts;
import io.github.clamentos.gattoslab.observability.metrics.entities.charts.RequestMetricsLatencyChart;
import io.github.clamentos.gattoslab.observability.metrics.entities.charts.RequestMetricsRateChart;
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.persistence.EntityField;
import io.github.clamentos.gattoslab.persistence.MongoClientWrapper;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.utils.GenericUtils;
import io.github.clamentos.gattoslab.utils.HttpUtils;
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
import org.bson.Document;
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
    private final int requestMetricsRetention;
    private final int systemMetricsRetention;
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

        ).getPeriod();

        batchScheduler.schedule(eventBus::trigger, "ObservabilityService::trigger", metricsConfig.getDumpToDbSchedule());
        batchScheduler.schedule(this::deleteOldMetrics, "ObservabilityService::deleteOldMetrics", metricsConfig.getRetentionSchedule());

        systemMetrics = new SystemMetrics(systemMetricsSamplingPeriod);

        siphonCapacity = metricsConfig.getSiphonCapacity();
        requestMetricsRetention = metricsConfig.getRequestMetricsRetention();
        systemMetricsRetention = metricsConfig.getSystemMetricsRetention();
        monitoredPaths = website.getPaths();

        this.mongoClientWrapper = mongoClientWrapper;

        primaryContext = new AtomicReference<>(new ObservabilityContext(eventBus, siphonCapacity));
        secondaryContext = new AtomicReference<>(new ObservabilityContext(eventBus, siphonCapacity));

        requestMetricsDumpFailures = new ConcurrentLinkedQueue<>();
        systemMetricsDumpFailures = new ConcurrentLinkedQueue<>();

        isHandlingEvent = new AtomicBoolean();
    }

    ///
    public void getRequestMetrics(final JsonGenerator generator, final RequestMetricsSearchFilter searchFilter) throws JacksonException, MongoException {

        final MongoCollection<RequestMetricsEntity> collection = mongoClientWrapper.getCollection(DatabaseCollection.REQUEST_METRICS);
        final Map<String, Map<Long, RequestMetricsAggregateEntity>> metricsMap = new HashMap<>();

        final List<Bson> pathsAggregation = new ArrayList<>();
        pathsAggregation.add(Aggregates.match(searchFilter.toBsonFilter()));
        pathsAggregation.addAll(AggregationPipelines.PERFORMANCE_METRICS);

        try(final MongoCursor<RequestMetricsAggregateEntity> entityCursor = collection.aggregate(pathsAggregation, RequestMetricsAggregateEntity.class).iterator()) {

            while(entityCursor.hasNext()) {

                final RequestMetricsAggregateEntity entity = entityCursor.next();
                metricsMap.computeIfAbsent(entity.getKey(), _ -> new TreeMap<>()).put(entity.getTimeSlot(), entity);
            }
        }

        final List<Long> labels = new ArrayList<>();

        for(final Map<Long, RequestMetricsAggregateEntity> metricsMapInner : metricsMap.values()) {

            for(long i = searchFilter.getStartTimestamp(); i < searchFilter.getEndTimestamp(); i += searchFilter.getBucketSize()) {

                labels.add(i);
                metricsMapInner.putIfAbsent(i, null);
            }
        }

        final List<ChartDataset<Integer>> rateDatasets = new ArrayList<>();
        final List<ChartDataset<BubbleChartDataEntry>> latencyDatasets = new ArrayList<>();

        for(final Map.Entry<String, Map<Long, RequestMetricsAggregateEntity>> metricsMapEntry : metricsMap.entrySet()) {

            final String key = metricsMapEntry.getKey();
            final Collection<RequestMetricsAggregateEntity> innerEntities = metricsMapEntry.getValue().values();

            final List<BubbleChartDataEntry> latencyData = new ArrayList<>();

            for(final RequestMetricsAggregateEntity entity : innerEntities) {

                final List<Long> latencyDistribution = entity.getLatencyDistribution();

                for(int i = 0; i < latencyDistribution.size(); i++) {

                    latencyData.add(new BubbleChartDataEntry(entity.getTimeSlot(), i, latencyDistribution.get(i)));
                }
            }

            rateDatasets.add(new ChartDataset<>(key, innerEntities.stream().map(RequestMetricsAggregateEntity::getRate).toList()));
            latencyDatasets.add(new ChartDataset<>(key, latencyData));
        }

        generator.writePOJO(new RequestMetricsCharts(new RequestMetricsRateChart(labels, rateDatasets), new RequestMetricsLatencyChart(latencyDatasets)));
    }

    ///..
    public void getInvocationMetrics(final JsonGenerator generator, final RequestMetricsSearchFilter searchFilter) throws JacksonException, MongoException {

        final MongoCollection<RequestMetricsEntity> collection = mongoClientWrapper.getCollection(DatabaseCollection.REQUEST_METRICS);

        final Bson sort = Aggregates.sort(Sorts.descending("count"));
        final Bson aggregation = Aggregates.match(searchFilter.toBsonFilter());
        final List<Bson> pathsAggregation = List.of(aggregation, AggregationPipelines.PATH_INVOCATIONS, sort);
        final List<Bson> userAgentsAggregation = List.of(aggregation, AggregationPipelines.USER_AGENTS, sort);

        try(

            final MongoCursor<PathInvocationAggregationEntity> paths = collection.aggregate(pathsAggregation, PathInvocationAggregationEntity.class).iterator();
            final MongoCursor<UserAgentAggregationEntity> userAgents = collection.aggregate(userAgentsAggregation, UserAgentAggregationEntity.class).iterator();
        ) {

            generator.writeStartObject();

            generator.writeArrayPropertyStart("paths");
            while(paths.hasNext()) generator.writePOJO(paths.next());
            generator.writeEndArray();

            generator.writeArrayPropertyStart("userAgents");
            while(userAgents.hasNext()) generator.writePOJO(userAgents.next());
            generator.writeEndArray();

            generator.writeEndObject();
        }
    }

    ///..
    public void getSystemMetrics(final JsonGenerator generator, final TemporalSearchFilter searchFilter) throws JacksonException, MongoException {

        // TODO: entitify & aggregate
        final MongoCollection<Document> collection = mongoClientWrapper.getCollection(DatabaseCollection.SYSTEM_METRICS);

        try(final MongoCursor<Document> metrics = collection.find(searchFilter.toBsonFilter()).sort(Sorts.ascending(EntityField.TIMESTAMP.getField())).iterator()) {

            generator.writeStartArray();
            while(metrics.hasNext()) generator.writePOJO(metrics.next());
            generator.writeEndArray();
        }
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

            if(success) break;
            else GenericUtils.sleep(1L);
        }
	}

    ///..
    public void close() throws IOException {

        this.dumpMetrics();
        systemMetrics.close();
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
            final Bson requestDeleteFilter = Filters.lte("timestamp", now - (requestMetricsRetention * 24 * 3600 * 1000));
            final Bson systemDeleteFilter = Filters.lte("timestamp", now - (systemMetricsRetention * 24 * 3600 * 1000));

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

        while(!context.isNoOneThere()) GenericUtils.sleep(1L);

        mongoClientWrapper.insertAll(context.drainSiphon(), DatabaseCollection.REQUEST_METRICS);
        context.reset();
    }

    ///
}
