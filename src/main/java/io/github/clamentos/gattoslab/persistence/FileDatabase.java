package io.github.clamentos.gattoslab.persistence;

///
import io.github.clamentos.gattoslab.configuration.dynamic.entities.DynamicPropertyEntity;
import io.github.clamentos.gattoslab.exceptions.CauseContainer;
import io.github.clamentos.gattoslab.observability.filters.SearchFilter;
import io.github.clamentos.gattoslab.observability.logging.entities.LogEntity;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

///..
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

///
public final class FileDatabase {

    ///
    private final JsonMapper jsonMapper;

    ///..
    private final ZoneId zoneId;
    private final DateTimeFormatter formatter;

    ///
    public FileDatabase(final JsonMapper jsonMapper) {

        this.jsonMapper = jsonMapper;

        zoneId = ZoneId.systemDefault();
        formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");
    }

    ///
    public <T extends SearchableEntity> List<T> fetchByFilter(final EntityType entityType, final SearchFilter searchFilter, final Class<T> clazz) throws IOException {

        final String startDate = formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(searchFilter.getStartTimestamp()), zoneId));
        final String endDate = formatter.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(searchFilter.getEndTimestamp()), zoneId));
        final List<Path> filesToSearch = new ArrayList<>();

        try(final Stream<Path> files = Files.list(entityType.getPath())) {

            filesToSearch.addAll(files

                .filter(p -> {

                    final String nameTemp = p.getFileName().toString().substring(entityType.getDateStartIndex(), entityType.getDateEndIndex());
                    return nameTemp.compareTo(startDate) >= 0 && nameTemp.compareTo(endDate) <= 0;
                })
                .toList()
            );
        }

        catch(final FileNotFoundException _) {

            return List.of();
        }

        final List<T> entities = new ArrayList<>();

        for(final Path file : filesToSearch) {

            try(final Stream<String> lines = Files.lines(file)) {

                lines.filter(line -> !line.isEmpty()).forEach(line -> {

                    final T entity = this.parseEntity(line, clazz);
                    if(entity.respectsFilter(searchFilter)) entities.add(entity);
                });
            }

            catch(final FileNotFoundException _) {

                return List.of();
            }

            catch(final JacksonException exc) {

                throw new IOException(GenericUtils.WRAPPED_EXCEPTION_MSG, new CauseContainer("FileDatabase.fetchByFilter", exc));
            }
        }

        return entities;
    }

    ///..
    public List<DynamicPropertyEntity> fetchDynamicProperties() throws IOException {

        try(final Stream<String> lines = Files.lines(EntityType.DYNAMIC_PROPERTIES.getPath())) {

            final List<DynamicPropertyEntity> entities = new ArrayList<>();

            lines.filter(line -> !line.isEmpty()).forEach(line -> {

                final DynamicPropertyEntity entity = this.parseEntity(line, DynamicPropertyEntity.class);
                if(entity.respectsFilter(null)) entities.add(entity);
            });

            return entities;
        }

        catch(final FileNotFoundException _) {

            return List.of();
        }

        catch(final JacksonException exc) {

            throw new IOException(GenericUtils.WRAPPED_EXCEPTION_MSG, new CauseContainer("FileDatabase.fetchDynamicProperties", exc));
        }
    }

    ///..
    private <T extends SearchableEntity> T parseEntity(final String line, final Class<T> clazz) throws JacksonException {

        if(clazz == LogEntity.class) return clazz.cast(new LogEntity(line));
        return jsonMapper.readValue(line, clazz);
    }

    ///
}
