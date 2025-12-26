package io.github.clamentos.gattoslab.observability.logging;

///
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

///.
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.json.JsonMapper;

///
@Service
@Slf4j

///
public final class LogsService {

    ///
    private final MongoClientWrapper mongoClientWrapper;
    private final JsonMapper jsonMapper;

    ///
    @Autowired
    public LogsService(final MongoClientWrapper mongoClientWrapper, final JsonMapper jsonMapper) {

        this.mongoClientWrapper = mongoClientWrapper;
        this.jsonMapper = jsonMapper;
    }

    ///
    public StreamingResponseBody getLogs(final LogSearchFilter logSearchFilter) throws MongoException {

        final MongoCollection<Document> logsCollection = mongoClientWrapper.getCollection(DatabaseCollection.LOGS);
        final MongoCursor<Document> cursor = logsCollection.find(logSearchFilter.toBsonFilter()).sort(Sorts.ascending("timestamp")).iterator();

        return outputStream -> {

            try(final JsonGenerator generator = jsonMapper.createGenerator(new CompressingOutputStream(outputStream))) {

                generator.writeStartArray();
                while(cursor.hasNext()) generator.writePOJO(cursor.next()); //
                generator.writeEndArray();
            }
        };
    }

    ///..
    public StreamingResponseBody getFallbackLogs() {

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

    ///
}
