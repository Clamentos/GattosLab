package io.github.clamentos.gattoslab.observability;

///
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

///.
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
import io.github.clamentos.gattoslab.observability.metrics.MetricsContainer;
import io.github.clamentos.gattoslab.observability.metrics.ObservabilityContext;
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.persistence.MongoClientWrapper;
import io.github.clamentos.gattoslab.utils.CompressingOutputStream;
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
import java.util.Set;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.bson.Document;

///..
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.ServletRequestHandledEvent;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

///
@Service
@Slf4j

///
public final class ObservabilityService implements HandlerInterceptor {

    ///
    private final Set<String> monitoredPaths;

    ///..
    private final ObservabilityContext observabilityContext;
    private final MongoClientWrapper mongoClientWrapper;
    private final ObjectMapper objectMapper;

    ///
    @Autowired
    public ObservabilityService(

        final PropertyProvider propertyProvider,
        final Website website,
        final ObservabilityContext observabilityContext,
        final MongoClientWrapper mongoClientWrapper,
        final ObjectMapper objectMapper

    ) throws PropertyNotFoundException {

        monitoredPaths = website.getPaths(null);

        this.observabilityContext = observabilityContext;
        this.mongoClientWrapper = mongoClientWrapper;
        this.objectMapper = objectMapper;
    }

    ///
    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {

        final String userAgent = request.getHeader("User-Agent");
        observabilityContext.updateUserAgentCounts(userAgent != null ? userAgent : "null");

        return true;
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
    @EventListener
    protected void handleRequestHandledEvent(final ServletRequestHandledEvent requestHandledEvent) {

        final String trueUrl = requestHandledEvent.getRequestUrl();
        observabilityContext.updatePathInvocations(trueUrl);

        observabilityContext.updateRequests(

            (int)requestHandledEvent.getProcessingTimeMillis(),
            (short)requestHandledEvent.getStatusCode(),
            monitoredPaths.contains(trueUrl) ? trueUrl : "<others>"
        );
    }

    ///..
    @Scheduled(cron = "${app.metrics.dumpToDbRate}")
    protected void dumpToDb() {

        final Pair<MetricsContainer, Map<DatabaseCollection, List<Document>>> context = observabilityContext.dumpToDb();
        final ClientSession session = mongoClientWrapper.getClient().startSession();

        try {

            session.startTransaction();

            for(final Map.Entry<DatabaseCollection, List<Document>> entity : context.getB().entrySet()) {

                final List<Document> documents = entity.getValue();
                if(!documents.isEmpty()) mongoClientWrapper.getCollection(entity.getKey()).insertMany(documents);
            }

            session.commitTransaction();
        }

        catch(final Exception exc) {

            log.error("Could not write metrics to DB", exc);

            session.abortTransaction();
            observabilityContext.merge(context.getA());
        }

        session.close();
    }

    ///.
    @SuppressWarnings("unchecked")
    private Map<String, MutableLong> getAbsoluteCounts(

        final long startTimestamp,
        final long endTimestamp,
        final DatabaseCollection mongoCollection

    ) throws MongoException {

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
    private StreamingResponseBody fetchMetrics(final DatabaseCollection databaseCollection, final SearchFilter searchFilter)
    throws MongoException {

        final MongoCollection<Document> collection = mongoClientWrapper.getCollection(databaseCollection);
        final MongoCursor<Document> results = collection.find(searchFilter.toBsonFilter()).sort(Sorts.ascending("timestamp")).iterator();

        return outputStream -> {

            try(final JsonGenerator generator = new JsonFactory(objectMapper).createGenerator(new CompressingOutputStream(outputStream))) {

                generator.writeStartArray();
                while(results.hasNext()) generator.writeObject(results.next());
                generator.writeEndArray();
            }
        };
    }

    ///
}
