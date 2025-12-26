package io.github.clamentos.gattoslab.observability;

///
import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

///.
import io.github.clamentos.gattoslab.configuration.PropertyProvider;
import io.github.clamentos.gattoslab.observability.filters.RequestMetricsSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.SearchFilter;
import io.github.clamentos.gattoslab.observability.filters.TemporalSearchFilter;
import io.github.clamentos.gattoslab.observability.metrics.DrainMetricsEvent;
import io.github.clamentos.gattoslab.observability.metrics.ObservabilityContext;
import io.github.clamentos.gattoslab.observability.metrics.SystemMetrics;
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.persistence.MongoClientWrapper;
import io.github.clamentos.gattoslab.utils.CompressingOutputStream;
import io.github.clamentos.gattoslab.utils.GenericUtils;
import io.github.clamentos.gattoslab.utils.MutableLong;
import io.github.clamentos.gattoslab.utils.Pair;
import io.github.clamentos.gattoslab.web.Website;

///.
import jakarta.el.PropertyNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

///.
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

///.
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.json.JsonMapper;

///
@Service
@Slf4j

///
public final class ObservabilityService implements HandlerInterceptor {

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
    private final Queue<Pair<ObservabilityContext, Document>> dumpFailures;
    private final AtomicBoolean isHandlingEvent;

    ///
    @Autowired
    public ObservabilityService(

        final PropertyProvider propertyProvider,
        final Website website,
        final SystemMetrics systemMetrics,
        final MongoClientWrapper mongoClientWrapper,
        final ApplicationEventPublisher applicationEventPublisher,
        final JsonMapper jsonMapper

    ) throws PropertyNotFoundException {

        siphonCapacity = propertyProvider.getProperty("app.metrics.siphonCapacity", Integer.class);
        monitoredPaths = website.getPaths();

        this.systemMetrics = systemMetrics;
        this.mongoClientWrapper = mongoClientWrapper;
        this.applicationEventPublisher = applicationEventPublisher;
        this.jsonMapper = jsonMapper;

        primaryContext = new AtomicReference<>(new ObservabilityContext(applicationEventPublisher, siphonCapacity));
        secondaryContext = new AtomicReference<>(new ObservabilityContext(applicationEventPublisher, siphonCapacity));
        dumpFailures = new ConcurrentLinkedQueue<>();
        isHandlingEvent = new AtomicBoolean();
    }

    ///
    @Override
    public void afterCompletion(final HttpServletRequest request, final HttpServletResponse response, final Object handler, final Exception exc) {

        final String trueUrl = request.getRequestURI();
        final String path = monitoredPaths.contains(trueUrl) ? trueUrl : "<others>";
        final String userAgent = GenericUtils.getOrDefault(request.getHeader("User-Agent"), "null");
        final int processingTime = (int)(System.currentTimeMillis() - (long)request.getAttribute("START_TIME_ATTRIBUTE"));

        while(true) {

            if(primaryContext.get().updateMetrics(processingTime, response.getStatus(), path, trueUrl, userAgent)) break;
            else GenericUtils.sleep(5L);
        }
	}

    ///..
    public Map<String, MutableLong> getPathInvocations(final TemporalSearchFilter searchFilter) throws MongoException {

        return this.getAbsoluteCounts(searchFilter.getStartTimestamp(), searchFilter.getEndTimestamp(), DatabaseCollection.PATHS_INVOCATIONS);
    }

    ///..
    public Map<String, MutableLong> getUserAgentsCount(final TemporalSearchFilter searchFilter) throws MongoException {

        return this.getAbsoluteCounts(searchFilter.getStartTimestamp(), searchFilter.getEndTimestamp(), DatabaseCollection.USER_AGENTS);
    }

    ///..
    public StreamingResponseBody getRequestMetrics(final RequestMetricsSearchFilter searchFilter) throws MongoException {

        return this.fetchMetrics(DatabaseCollection.REQUEST_METRICS, searchFilter);
    }

    ///..
    public StreamingResponseBody getSystemMetrics(final TemporalSearchFilter searchFilter) {

        return this.fetchMetrics(DatabaseCollection.SYSTEM_METRICS, searchFilter);
    }

    ///.
    @Scheduled(cron = "${app.metrics.dumpToDbSchedule}", scheduler = "batchScheduler")
    protected void dumpToDb() {

        applicationEventPublisher.publishEvent(new DrainMetricsEvent());
    }

    ///..
    @EventListener
    protected void handleDrainEvent(final DrainMetricsEvent event) {

        if(isHandlingEvent.compareAndSet(false, true)) {

            final ObservabilityContext oldPrimary = this.swapContexts();
            final Document systemMetricsSample = systemMetrics.toDocument();
            final ClientSession session = mongoClientWrapper.getClient().startSession();

            try {

                session.startTransaction();
                this.insertMetrics(oldPrimary, systemMetricsSample);

                final Pair<ObservabilityContext, Document> failureEntry = dumpFailures.peek();
                if(failureEntry != null) this.insertMetrics(failureEntry.getA(), failureEntry.getB());

                session.commitTransaction();
            }

            catch(final Exception exc) {

                log.error("Could not write metrics to DB", exc);

                session.abortTransaction();
                dumpFailures.add(new Pair<>(oldPrimary, systemMetricsSample));
                secondaryContext.set(new ObservabilityContext(applicationEventPublisher, siphonCapacity));
            }

            session.close();
            dumpFailures.poll();
            isHandlingEvent.set(false);
        }
    }

    ///.
    @SuppressWarnings("unchecked")
    private Map<String, MutableLong> getAbsoluteCounts(final long startTimestamp, final long endTimestamp, final DatabaseCollection mongoCollection) 
    throws MongoException {

        final Map<String, MutableLong> result = new HashMap<>();
        final MongoCollection<Document> collection = mongoClientWrapper.getCollection(mongoCollection);

        final MongoCursor<Document> results = collection

            .find(Filters.and(
                Filters.gte("timestamp", startTimestamp),
                Filters.lte("timestamp", endTimestamp))
            )
            .iterator()
        ;

        while(results.hasNext()) {

            final List<Object> elements = results.next().getList("elements", Object.class);

            for(final Object element : elements) {

                final Map<String, Object> elemMap = (Map<String, Object>)element;
                result.computeIfAbsent((String)elemMap.get("name"), _ -> new MutableLong()).increment((Integer)elemMap.get("count"));
            }
        }

        return result;
    }

    ///..
    private StreamingResponseBody fetchMetrics(final DatabaseCollection databaseCollection, final SearchFilter searchFilter) throws MongoException {

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
    private ObservabilityContext swapContexts() {

        final ObservabilityContext primary = primaryContext.get();

        primaryContext.set(secondaryContext.get());
        secondaryContext.set(primary);

        return primary;
    }

    ///..
    private void insertMetrics(final ObservabilityContext context, final Document systemMetricsSample) throws MongoException {

        while(!context.isNoOneThere()) GenericUtils.sleep(5L);
        mongoClientWrapper.getCollection(DatabaseCollection.SYSTEM_METRICS).insertOne(systemMetricsSample);

        for(final Map.Entry<DatabaseCollection, List<Document>> entity : context.toDocuments().entrySet()) {

            final List<Document> documents = entity.getValue();
            if(!documents.isEmpty()) mongoClientWrapper.getCollection(entity.getKey()).insertMany(documents);
        }

        context.reset();
    }

    ///
}
