package io.github.clamentos.gattoslab.observability;

///
import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Sorts;

///.
import io.github.clamentos.gattoslab.configuration.PropertyProvider;
import io.github.clamentos.gattoslab.observability.filters.RequestMetricsSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.SearchFilter;
import io.github.clamentos.gattoslab.observability.filters.TemporalSearchFilter;
import io.github.clamentos.gattoslab.observability.metrics.DrainMetricsEvent;
import io.github.clamentos.gattoslab.observability.metrics.ObservabilityContext;
import io.github.clamentos.gattoslab.observability.metrics.SystemMetrics;
import io.github.clamentos.gattoslab.observability.metrics.entries.PathInvocationsEntry;
import io.github.clamentos.gattoslab.observability.metrics.entries.TrackerEntry;
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.persistence.MongoClientWrapper;
import io.github.clamentos.gattoslab.utils.CompressingOutputStream;
import io.github.clamentos.gattoslab.utils.GenericUtils;
import io.github.clamentos.gattoslab.web.Website;

///.
import jakarta.annotation.PreDestroy;
import jakarta.el.PropertyNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

///.
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.bson.Document;

///..
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

///..
import org.apache.catalina.connector.ClientAbortException;

///..
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

///.
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.json.JsonMapper;

///
@Service
@Slf4j

///
public class ObservabilityService implements HandlerInterceptor {

    ///
    private final int siphonCapacity;
    private final Set<String> monitoredPaths;

    ///..
    private final SystemMetrics systemMetrics;
    private final MongoClientWrapper mongoClientWrapper;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final JsonMapper jsonMapper;

    ///..
    private final AtomicReference<ObservabilityContext> primaryContext;
    private final AtomicReference<ObservabilityContext> secondaryContext;

    private final Queue<ObservabilityContext> requestMetricsDumpFailures;
    private final Queue<Document> systemMetricsDumpFailures;

    private final AtomicBoolean isHandlingEvent;

    ///
    @Autowired
    public ObservabilityService(

        @NonNull final PropertyProvider propertyProvider,
        @NonNull final Website website,
        @NonNull final SystemMetrics systemMetrics,
        @NonNull final MongoClientWrapper mongoClientWrapper,
        @NonNull final ApplicationEventPublisher applicationEventPublisher,
        @NonNull final JsonMapper jsonMapper

    ) throws PropertyNotFoundException {

        siphonCapacity = propertyProvider.getProperty("app.metrics.siphonCapacity", Integer.class);
        monitoredPaths = website.getPaths();

        this.systemMetrics = systemMetrics;
        this.mongoClientWrapper = mongoClientWrapper;
        this.applicationEventPublisher = applicationEventPublisher;
        this.jsonMapper = jsonMapper;

        primaryContext = new AtomicReference<>(new ObservabilityContext(applicationEventPublisher, siphonCapacity));
        secondaryContext = new AtomicReference<>(new ObservabilityContext(applicationEventPublisher, siphonCapacity));

        requestMetricsDumpFailures = new ConcurrentLinkedQueue<>();
        systemMetricsDumpFailures = new ConcurrentLinkedQueue<>();

        isHandlingEvent = new AtomicBoolean();
    }

    ///
    public @NonNull List<TrackerEntry> getPathInvocations(@NonNull final TemporalSearchFilter searchFilter) throws MongoException {

        return this.getAbsoluteCounts(searchFilter, DatabaseCollection.PATH_INVOCATIONS);
    }

    ///..
    public @NonNull List<TrackerEntry> getUserAgentsCount(@NonNull final TemporalSearchFilter searchFilter) throws MongoException {

        return this.getAbsoluteCounts(searchFilter, DatabaseCollection.USER_AGENTS);
    }

    ///..
    public @NonNull StreamingResponseBody getRequestMetrics(@NonNull final RequestMetricsSearchFilter searchFilter) throws MongoException {

        return this.fetchMetrics(DatabaseCollection.REQUEST_METRICS, searchFilter);
    }

    ///..
    public @NonNull StreamingResponseBody getSystemMetrics(@NonNull final TemporalSearchFilter searchFilter) {

        return this.fetchMetrics(DatabaseCollection.SYSTEM_METRICS, searchFilter);
    }

    ///..
    @Override
    public void afterCompletion(

        @NonNull final HttpServletRequest request,
        @NonNull final HttpServletResponse response,
        @Nullable final Object handler,
        @Nullable final Exception exc
    ) {

        if(exc != null && (exc instanceof ClientAbortException || exc.getCause() instanceof ClientAbortException)) return;

        final String trueUrl = request.getRequestURI();
        final String path = monitoredPaths.contains(trueUrl) ? trueUrl : "<others>";
        final String userAgent = GenericUtils.getOrDefault(request.getHeader("User-Agent"), "null");

        while(true) {

            final boolean success = primaryContext.get().updateMetrics(

                (long)request.getAttribute("START_TIME_ATTRIBUTE"),
                System.currentTimeMillis(),
                response.getStatus(),
                path, trueUrl, userAgent
            );

            if(success) break;
            else GenericUtils.sleep(1L);
        }
	}

    ///.
    @Scheduled(cron = "${app.metrics.dumpToDbSchedule}", scheduler = "batchScheduler")
    protected void dumpToDb() {

        applicationEventPublisher.publishEvent(new DrainMetricsEvent());
    }

    ///..
    @Scheduled(cron = "${app.metrics.systemMetricsSampling}", scheduler = "batchScheduler")
    protected void sampleSystemMetrics() {

        final ClientSession session = mongoClientWrapper.getClient().startSession();
        Document toSave = systemMetrics.toDocument();

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
    @EventListener
    @Async("batchScheduler")
    protected void handleDrainEvent(@Nullable final DrainMetricsEvent event) {

        this.dumpMetrics();
    }

    ///..
    @PreDestroy
    protected void flushMetricsBeforeQuitting() {

        this.dumpMetrics();
    }

    ///.
    private @NonNull List<TrackerEntry> getAbsoluteCounts(@NonNull final TemporalSearchFilter searchFilter, @NonNull final DatabaseCollection mongoCollection)
    throws MongoException {

        final MongoCollection<Document> collection = mongoClientWrapper.getCollection(mongoCollection);
        final MongoCursor<Document> results = collection.find(searchFilter.toBsonFilter()).iterator();
        final Map<String, List<TrackerEntry>> groupByKey = new HashMap<>();

        while(results.hasNext()) {

            final TrackerEntry tracker = mongoCollection == DatabaseCollection.PATH_INVOCATIONS ?

                new PathInvocationsEntry(results.next()) :
                new TrackerEntry(results.next())
            ;

            groupByKey.computeIfAbsent(tracker.getKey(), _ -> new ArrayList<>()).add(tracker);
        }

        final List<TrackerEntry> mergedTrackers = new ArrayList<>();

        for(final Map.Entry<String, List<TrackerEntry>> entry : groupByKey.entrySet()) {

            final List<TrackerEntry> entries = entry.getValue();
            final TrackerEntry tracker = entries.get(0);

            for(int i = 1; i < entries.size(); i++) {

                tracker.merge(entries.get(i));
            }

            mergedTrackers.add(tracker);
        }

        return mergedTrackers;
    }

    ///..
    private @NonNull StreamingResponseBody fetchMetrics(@NonNull final DatabaseCollection databaseCollection, @NonNull final SearchFilter searchFilter)
    throws MongoException {

        final MongoCollection<Document> collection = mongoClientWrapper.getCollection(databaseCollection);
        final MongoCursor<Document> results = collection.find(searchFilter.toBsonFilter()).sort(Sorts.ascending("timestamp")).iterator();

        return outputStream -> {

            try(final JsonGenerator generator = jsonMapper.createGenerator(new CompressingOutputStream(outputStream))) {

                generator.writeStartArray();
                while(results.hasNext()) generator.writePOJO(results.next());
                generator.writeEndArray();
            }
        };
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
                secondaryContext.set(new ObservabilityContext(applicationEventPublisher, siphonCapacity));
            }

            session.close();
            isHandlingEvent.set(false);
        }
    }

    ///..
    private @NonNull ObservabilityContext swapContexts() {

        final ObservabilityContext primary = primaryContext.get();

        primaryContext.set(secondaryContext.get());
        secondaryContext.set(primary);

        return primary;
    }

    ///..
    private void insertMetrics(@NonNull final ObservabilityContext context) throws MongoException {

        while(!context.isNoOneThere()) GenericUtils.sleep(1L);

        for(final Map.Entry<DatabaseCollection, List<Document>> entity : context.toDocuments().entrySet()) {

            mongoClientWrapper.insertAll(entity.getValue(), entity.getKey());
        }

        context.reset();
    }

    ///
}
