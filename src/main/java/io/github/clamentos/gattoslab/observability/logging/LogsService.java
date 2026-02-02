package io.github.clamentos.gattoslab.observability.logging;

///
import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;

///.
import io.github.clamentos.gattoslab.configuration.PropertyProvider;
import io.github.clamentos.gattoslab.observability.filters.LogSearchFilter;
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.persistence.MongoClientWrapper;
import io.github.clamentos.gattoslab.utils.CompressingOutputStream;

///.
import java.io.BufferedReader;
import java.io.FileReader;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.bson.Document;
import org.bson.conversions.Bson;

///..
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

///..
import org.jspecify.annotations.NonNull;

///.
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.json.JsonMapper;

///
@Service
@Slf4j

///
public final class LogsService {

    ///
    private final int logsRetention;

    ///..
    private final MongoClientWrapper mongoClientWrapper;
    private final JsonMapper jsonMapper;

    ///
    @Autowired
    public LogsService(

        @NonNull final PropertyProvider propertyProvider,
        @NonNull final MongoClientWrapper mongoClientWrapper,
        @NonNull final JsonMapper jsonMapper
    ) {

        logsRetention = propertyProvider.getProperty("app.logs.logsRetention", Integer.class);

        this.mongoClientWrapper = mongoClientWrapper;
        this.jsonMapper = jsonMapper;
    }

    ///
    public @NonNull StreamingResponseBody getLogs(@NonNull final LogSearchFilter logSearchFilter) throws MongoException {

        final MongoCollection<Document> logsCollection = mongoClientWrapper.getCollection(DatabaseCollection.LOGS);
        final MongoCursor<Document> cursor = logsCollection.find(logSearchFilter.toBsonFilter()).sort(Sorts.ascending("timestamp")).iterator();

        return outputStream -> {

            try(final JsonGenerator generator = jsonMapper.createGenerator(new CompressingOutputStream(outputStream))) {

                generator.writeStartArray();
                while(cursor.hasNext()) generator.writePOJO(cursor.next());
                generator.writeEndArray();
            }
        };
    }

    ///..
    public @NonNull StreamingResponseBody getFallbackLogs() {

        return outputStream -> {

            try(
                final JsonGenerator generator = jsonMapper.createGenerator(new CompressingOutputStream(outputStream));
                final BufferedReader fileReader = new BufferedReader(new FileReader(MongoAppender.FALLBACK_FILE_PATH))
            ) {

                generator.writeStartArray();
                generator.writeString(fileReader.readLine());
                generator.writeEndArray();
            }
        };
    }

    ///.
    @Scheduled(cron = "${app.logs.retentionSchedule}", scheduler = "batchScheduler")
    protected void deleteOldMetrics() {

        log.info("Begin delete logs by retention");

        final ClientSession session = mongoClientWrapper.getClient().startSession();
        long deleted = 0;

        try {

            final long now = System.currentTimeMillis();
            final Bson logsDeleteFilter = Filters.lte("timestamp", now - (logsRetention * 24 * 3600 * 1000));

            session.startTransaction();
            deleted = mongoClientWrapper.getCollection(DatabaseCollection.LOGS).deleteMany(logsDeleteFilter).getDeletedCount();
            session.commitTransaction();
        }

        catch(final Exception exc) {

            log.error("Could not delete old logs from DB", exc);
            session.abortTransaction();
        }

        log.info("End delete metrics by retention, deleted {} logs", deleted);
        session.close();
    }

    ///
}
