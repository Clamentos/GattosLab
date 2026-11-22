package io.github.clamentos.gattoslab.observability.database;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.SortedList;
import io.github.clamentos.gattoslab.datastructures.StreamWriter;
import io.github.clamentos.gattoslab.datastructures.interfaces.Entity;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

///..
import lombok.Getter;

///
public final class ObservabilityDirectory<T extends Entity> {

    ///
    private final Logger logger;

    ///..
    @Getter
    private final Path directory;

    private final String name;
    private final String extension;

    ///
    public ObservabilityDirectory(final Logger logger, final Path fullPath) {

        this.logger = logger;
        this.directory = fullPath.getParent();

        final String fileName = fullPath.getFileName().toString();
        final int index = fileName.indexOf('.');

        if(index != -1) {

            this.name = fileName.substring(0, index) + "_";
            this.extension = fileName.substring(index, fileName.length());
        }

        else if(!fileName.isEmpty()) {

            this.name = fileName;
            this.extension = "";
        }

        else {

            this.name = "";
            this.extension = "";
        }
    }

    ///
    public void write(final List<T> entities, final boolean doSort) throws IOException {

        final int length = entities.size();
        final Map<Long, List<T>> entitiesByTheHour = HashMap.newHashMap(2);

        for(int i = 0; i < length; i++) {

            final T entity = entities.get(i);
            final long timestamp = entity.getTimestamp();

            entitiesByTheHour

                .computeIfAbsent(

                    (timestamp / 3_600_000) * 3_600_000,
                    _ -> doSort ? new SortedList<>((a, b) -> (int)(a.getTimestamp() - b.getTimestamp())) : new ArrayList<>()
                )
                .add(entity)
            ;
        }

        for(final Entry<Long, List<T>> entry : entitiesByTheHour.entrySet()) {

            final LocalDateTime window = LocalDateTime.ofInstant(Instant.ofEpochMilli(entry.getKey()), GenericUtils.DEFAULT_ZONE_ID);
            final List<T> groupedEntities = entry.getValue();
            final int groupedEntitiesLength = groupedEntities.size();

            try(final StreamWriter writer = this.openForWrite(window)) {

                for(int i = 0; i < groupedEntitiesLength; i++) {

                    groupedEntities.get(i).stream(writer);
                    writer.write('\n');
                }
            }
        }
    }

    ///..
    public int deleteAll(final LocalDateTime boundaryDate) throws IOException {

        try(final Stream<Path> files = Files.list(Path.of(this.toString()))) {

            final String boundaryFilePath = this.composePath(boundaryDate).toString();
            final int[] counterReference = new int[]{0};

            files.filter(path -> path.toString().compareTo(boundaryFilePath) < 0).forEach(path -> {

                try {

                    Files.delete(path);
                    counterReference[0]++;
                }

                catch(final IOException exc) {

                    if(this.logger != null) this.logger.error("Could not delete", exc);
                    else System.err.println("Could not delete because: " + exc);
                }
            });

            return counterReference[0];
        }

        catch(final NoSuchFileException _) {

            return 0;
        }
    }

    ///..
    public Path composePath(final LocalDateTime dateTime) {

        return Path.of(this.directory.toString(), this.name + dateTime.toString().substring(0, 13) + this.extension);
    }

    ///.
    private StreamWriter openForWrite(final LocalDateTime window) throws IOException {

        Files.createDirectories(this.directory);

        return new StreamWriter(

            Files.newOutputStream(this.composePath(window), StandardOpenOption.CREATE, StandardOpenOption.APPEND),
            ApplicationProperties.DEFAULT_STREAM_WRITER_BUFFER_SIZE
        );
    }

    ///
}
