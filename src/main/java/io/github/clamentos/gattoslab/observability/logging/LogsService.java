package io.github.clamentos.gattoslab.observability.logging;

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
import com.mongodb.client.result.DeleteResult;

///.
import io.github.clamentos.gattoslab.observability.filters.LogSearchFilter;
import io.github.clamentos.gattoslab.persistence.MongoClientWrapper;
import io.github.clamentos.gattoslab.utils.PropertyProvider;
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;

///.
import jakarta.el.PropertyNotFoundException;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.bson.Document;

///..
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

///
@Service
@Slf4j

///
public final class LogsService {

    ///
    private final long retention;

    ///.
    private final MongoClientWrapper mongoClientWrapper;
    private final ObjectMapper objectMapper;

    ///
    @Autowired
    public LogsService(final MongoClientWrapper mongoClientWrapper, final ObjectMapper objectMapper, final PropertyProvider propertyProvider)
    throws PropertyNotFoundException {

        retention = propertyProvider.getProperty("app.retention.value", Long.class) * 1000 * 60 * 60 * 24;

        this.mongoClientWrapper = mongoClientWrapper;
        this.objectMapper = objectMapper;
    }

    ///
    public StreamingResponseBody getLogs(final LogSearchFilter logSearchFilter) throws MongoException {

        final MongoCollection<Document> logsCollection = mongoClientWrapper.getCollection(DatabaseCollection.LOGS);
        final MongoCursor<Document> cursor = logsCollection.find(logSearchFilter.toBsonFilter()).iterator();

        return outputStream -> {

            try(final JsonGenerator generator = new JsonFactory(objectMapper).createGenerator(outputStream)) {

                generator.writeStartArray();
                while(cursor.hasNext()) generator.writeObject(cursor.next());
                generator.writeEndArray();
            }
        };
    }

    ///.
    @Scheduled(cron = "${app.retention.cleanSchedule}")
    protected void cleanOldLogs() {

        final ClientSession session = mongoClientWrapper.getClient().startSession();

        try {

            session.startTransaction();

            final MongoCollection<Document> logsCollection = mongoClientWrapper.getCollection(DatabaseCollection.LOGS);
            final DeleteResult result = logsCollection.deleteMany(Filters.lte("timestamp", System.currentTimeMillis() - retention));
            final long count = result.getDeletedCount();

            session.commitTransaction();
            log.info("Logs cleaned: {}", count);
        }

        catch(final MongoException exc) {

            session.abortTransaction();
            log.error("Could not delete logs", exc);
        }

        session.close();
    }

    ///
}
