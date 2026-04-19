package io.github.clamentos.gattoslab.observability.logging;

///
import com.mongodb.MongoException;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Sorts;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.pojos.LogsConfig;
import io.github.clamentos.gattoslab.exceptions.ValidationException;
import io.github.clamentos.gattoslab.http.ResponseSender;
import io.github.clamentos.gattoslab.observability.filters.LogSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.TemporalSearchFilter;
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.persistence.EntityField;
import io.github.clamentos.gattoslab.persistence.MongoClientWrapper;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;

///..
import java.io.BufferedReader;
import java.io.FileReader;

///..
import lombok.extern.slf4j.Slf4j;

///..
import org.bson.Document;

///..
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;

///
@Slf4j

///
public final class LogsService {

    ///
    private final long logsRetention;

    ///..
    private final MongoClientWrapper mongoClientWrapper;

    ///
    public LogsService(final ApplicationProperties applicationProperties, final BatchScheduler batchScheduler, final MongoClientWrapper mongoClientWrapper)
    throws IllegalArgumentException {

        final LogsConfig logsConfig = applicationProperties.getLogsConfig();

        logsRetention = logsConfig.getRetention().toMillis();
        batchScheduler.schedule(this::deleteOldLogs, "LogsService::deleteOldLogs", logsConfig.getRetentionSchedule());

        this.mongoClientWrapper = mongoClientWrapper;
    }

    ///
    public ResponseSender getLogs(final JsonGenerator generator, final LogSearchFilter logSearchFilter) throws JacksonException, MongoException, ValidationException {

        if(logSearchFilter.getStartTimestamp() < logSearchFilter.getEndTimestamp()) {

            throw new ValidationException(EntityField.START_TIMESTAMP + " cannot be smaller than " + EntityField.END_TIMESTAMP);
        }

        final MongoCollection<Document> logsCollection = mongoClientWrapper.getCollection(DatabaseCollection.LOGS);

        final MongoCursor<Document> cursor = logsCollection

            .find(logSearchFilter.toBsonFilter())
            .sort(Sorts.ascending(EntityField.TIMESTAMP))
            .batchSize(500)
            .iterator()
        ;

        return () -> {

            try(cursor) {

                generator.writeStartArray();
                while(cursor.hasNext()) generator.writePOJO(cursor.next());
                generator.writeEndArray();
            }
        };
    }

    ///..
    public ResponseSender getFallbackLogs(final JsonGenerator generator) throws JacksonException {

        return () -> {

            try(final BufferedReader fileReader = new BufferedReader(new FileReader(MongoAppender.FALLBACK_FILE_PATH))) {

                generator.writeStartArray();
                generator.writeString(fileReader.readLine());
                generator.writeEndArray();
            }
        };
    }

    ///.
    private void deleteOldLogs() {

        log.info("Begin delete logs by retention");

        final ClientSession session = mongoClientWrapper.getClient().startSession();
        long deleted = 0;

        try {

            final long now = System.currentTimeMillis();
            final TemporalSearchFilter filter = new TemporalSearchFilter(now - logsRetention, now);

            session.startTransaction();
            deleted = mongoClientWrapper.getCollection(DatabaseCollection.LOGS).deleteMany(filter.toBsonFilter()).getDeletedCount();
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
