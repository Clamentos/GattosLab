package io.github.clamentos.gattoslab.observability.logging;

///
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;

///..
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Sorts;

///.
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

///..
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

///
@Service
@Slf4j

///
public final class LogsService {

    ///
    private final MongoClientWrapper mongoClientWrapper;
    private final ObjectMapper objectMapper;

    ///
    @Autowired
    public LogsService(final MongoClientWrapper mongoClientWrapper, final ObjectMapper objectMapper) {

        this.mongoClientWrapper = mongoClientWrapper;
        this.objectMapper = objectMapper;
    }

    ///
    public StreamingResponseBody getLogs(final LogSearchFilter logSearchFilter) throws MongoException {

        final MongoCollection<Document> logsCollection = mongoClientWrapper.getCollection(DatabaseCollection.LOGS);
        final MongoCursor<Document> cursor = logsCollection.find(logSearchFilter.toBsonFilter()).sort(Sorts.ascending("timestamp")).iterator();

        return outputStream -> {

            try(final JsonGenerator generator = new JsonFactory(objectMapper).createGenerator(new CompressingOutputStream(outputStream))) {

                generator.writeStartArray();
                while(cursor.hasNext()) generator.writeObject(cursor.next());
                generator.writeEndArray();
            }
        };
    }

    ///..
    public StreamingResponseBody getFallbackLogs() {

        return outputStream -> {

            try(
                final JsonGenerator generator = new JsonFactory(objectMapper).createGenerator(new CompressingOutputStream(outputStream));
                final BufferedReader fileReader = new BufferedReader(new FileReader(MongoAppender.FALLBACK_FILE_PATH))
            ) {

                generator.writeStartArray();
                generator.writeString(fileReader.readLine());
                generator.writeEndArray();
            }
        };
    }

    ///
}
