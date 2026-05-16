package io.github.clamentos.gattoslab.observability.logging;

///
import io.github.clamentos.gattoslab.exceptions.ValidationException;
import io.github.clamentos.gattoslab.http.ResponseSender;
import io.github.clamentos.gattoslab.observability.filters.LogSearchFilter;
import io.github.clamentos.gattoslab.observability.logging.entities.LogEntity;
import io.github.clamentos.gattoslab.persistence.EntityType;
import io.github.clamentos.gattoslab.persistence.FileDatabase;

///..
import java.io.IOException;
import java.util.List;

///..
import lombok.extern.slf4j.Slf4j;

///..
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;

///
@Slf4j

///
public final class LogsService {

    ///
    private final FileDatabase fileDatabase;

    ///
    public LogsService(final FileDatabase fileDatabase) throws IllegalArgumentException {

        this.fileDatabase = fileDatabase;
    }

    ///
    public ResponseSender getLogs(final JsonGenerator generator, final LogSearchFilter logSearchFilter) throws IOException, JacksonException, ValidationException {

        if(logSearchFilter.getStartTimestamp() > logSearchFilter.getEndTimestamp()) {

            throw new ValidationException("Field 'endTimestamp' cannot be smaller than 'startTimestamp'", "LogsService.getLogs");
        }

        final List<LogEntity> logs = fileDatabase.fetchByFilter(EntityType.LOGS, logSearchFilter, LogEntity.class);
        logs.sort((a, b) -> Math.clamp(b.getTimestamp() - a.getTimestamp(), Integer.MIN_VALUE, Integer.MAX_VALUE));

        return () -> {

            generator.writeStartArray();
            for(final LogEntity log : logs) generator.writePOJO(log);
            generator.writeEndArray();
        };
    }

    ///
}
