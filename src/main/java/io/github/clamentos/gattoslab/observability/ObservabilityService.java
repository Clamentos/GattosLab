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
import com.mongodb.client.result.DeleteResult;

///.
import io.github.clamentos.gattoslab.observability.filters.ChartSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.TemporalSearchFilter;
import io.github.clamentos.gattoslab.observability.metrics.MetricsContainer;
import io.github.clamentos.gattoslab.observability.metrics.ObservabilityContext;
import io.github.clamentos.gattoslab.observability.metrics.system.SystemStatus;
import io.github.clamentos.gattoslab.persistence.MongoClientWrapper;
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.utils.CompressingOutputStream;
import io.github.clamentos.gattoslab.utils.MutableLong;
import io.github.clamentos.gattoslab.utils.PropertyProvider;
import io.github.clamentos.gattoslab.web.StaticSite;

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
    private final long retention;
    private final Set<String> sitePaths;

    ///..
    private final ObservabilityContext observabilityContext;
    private final MongoClientWrapper mongoClientWrapper;
    private final ObjectMapper objectMapper;

    ///
    @Autowired
    public ObservabilityService(

        final StaticSite staticSite,
        final ObservabilityContext observabilityContext,
        final MongoClientWrapper mongoClientWrapper,
        final ObjectMapper objectMapper,
        final PropertyProvider propertyProvider

    ) throws PropertyNotFoundException {

        retention = propertyProvider.getProperty("app.retention.value", Long.class) * 1000 * 60 * 60 * 24;
        sitePaths = staticSite.getPaths();

        this.observabilityContext = observabilityContext;
        this.mongoClientWrapper = mongoClientWrapper;
        this.objectMapper = objectMapper;
    }

    ///
    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {

        final String userAgent = request.getHeader("User-Agent");
        observabilityContext.updateUserAgents(userAgent != null ? userAgent : "null");

        return true;
    }

    ///..
    public Map<String, MutableLong> getPathsCount(final TemporalSearchFilter searchFilter) throws MongoException {

        return this.getAbsoluteCounts(

            searchFilter.getStartTimestamp(),
            searchFilter.getEndTimestamp(),
            DatabaseCollection.PATHS_INVOCATIONS
        );
    }

    ///..
    public Map<String, MutableLong> getUserAgentsCount(final TemporalSearchFilter searchFilter) throws MongoException {

        return this.getAbsoluteCounts(

            searchFilter.getStartTimestamp(),
            searchFilter.getEndTimestamp(),
            DatabaseCollection.USER_AGENTS
        );
    }

    ///..
    public StreamingResponseBody getCharts(final ChartSearchFilter chartSearchFilter) throws MongoException {

        final MongoCollection<Document> collection = mongoClientWrapper.getCollection(DatabaseCollection.CHARTS);
        final MongoCursor<Document> results = collection.find(chartSearchFilter.toBsonFilter()).sort(Sorts.ascending("timestamp")).iterator();

        return outputStream -> {

            try(final JsonGenerator generator = new JsonFactory(objectMapper).createGenerator(new CompressingOutputStream(outputStream))) {

                generator.writeStartArray();
                while(results.hasNext()) generator.writeObject(results.next());
                generator.writeEndArray();
            }
        };
    }

    ///..
    public SystemStatus getJvmMetrics() {

        return this.observabilityContext.getJvmMetrics();
    }

    ///.
    @EventListener
    protected void handleRequestHandledEvent(final ServletRequestHandledEvent requestHandledEvent) {

        final String trueUrl = requestHandledEvent.getRequestUrl();

        observabilityContext.updateRequests(
    
            requestHandledEvent.getStatusCode(),
            sitePaths.contains(trueUrl) ? trueUrl : "<other>",
            requestHandledEvent.getMethod() + trueUrl,
            (int)requestHandledEvent.getProcessingTimeMillis()
        );
    }

    ///..
    //@Scheduled(fixedRateString = "${app.metrics.dumpToDbRate}")
    protected void dumpToDb() {

        final MetricsContainer container = observabilityContext.advance();

        if(container != null) {

            final ClientSession session = mongoClientWrapper.getClient().startSession();

            try {

                session.startTransaction();

                for(final Map.Entry<DatabaseCollection, List<Document>> entity : container.toDocuments().entrySet()) {

                    final List<Document> documents = entity.getValue();
                    if(!documents.isEmpty()) mongoClientWrapper.getCollection(entity.getKey()).insertMany(documents);
                }

                session.commitTransaction();
            }

            catch(final Exception exc) {

                log.error("Could not write charts to DB", exc);
            }

            session.close();
        }
    }

    ///..
    @Scheduled(cron = "${app.retention.cleanSchedule}")
    protected void cleanOldMetrics() {

        final List<DatabaseCollection> collectionsToClean = List.of(

            DatabaseCollection.CHARTS,
            DatabaseCollection.PATHS_INVOCATIONS,
            DatabaseCollection.USER_AGENTS
        );

        final ClientSession session = mongoClientWrapper.getClient().startSession();

        try {

            for(final DatabaseCollection databaseCollection : collectionsToClean) {

                session.startTransaction();

                final MongoCollection<Document> logsCollection = mongoClientWrapper.getCollection(databaseCollection);
                final DeleteResult result = logsCollection.deleteMany(Filters.lte("timestamp", System.currentTimeMillis() - retention));
                final long count = result.getDeletedCount();

                session.commitTransaction();
                log.info("{} cleaned: {}", databaseCollection.getValue(), count);
            }
        }

        catch(final MongoException exc) {

            session.abortTransaction();
            log.error("Could not delete metrics", exc);
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

        final MongoCollection<Document> collection = mongoClientWrapper.getCollection(mongoCollection);
        final MongoCursor<Document> results = collection

            .find(Filters.and(
                Filters.gte("timestamp", startTimestamp),
                Filters.lte("timestamp", endTimestamp))
            )
            .iterator()
        ;

        final Map<String, MutableLong> result = new HashMap<>();

        while(results.hasNext()) {

            final List<Object> elements = results.next().getList("elements", Object.class);

            for(final Object element : elements) {

                final Map<String, Object> elemMap = (Map<String, Object>)element;
                result.computeIfAbsent((String)elemMap.get("name"), _ -> new MutableLong()).increment((Integer)elemMap.get("count"));
            }
        }

        return result;
    }

    ///
}
