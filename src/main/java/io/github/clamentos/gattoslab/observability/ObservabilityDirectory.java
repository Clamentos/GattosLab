package io.github.clamentos.gattoslab.observability;

import io.github.clamentos.gattoslab.datastructures.SortedList;
///
import io.github.clamentos.gattoslab.http.server.StreamWriter;
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

///
public final class ObservabilityDirectory<T extends Entity> {

    ///
    private final Logger logger;

    ///..
    private final Path directoryPath;
    private final List<String> fileNameComponents;

    ///
    public ObservabilityDirectory(final Logger logger, final Path fullPath) {

        this.logger = logger;

        this.directoryPath = fullPath.getParent();
        this.fileNameComponents = GenericUtils.fastSplit(fullPath.toString(), '.');
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

            final String boundaryFileName = this.composeName(boundaryDate);
            final int[] counterRef = new int[]{0};

            files.filter(path -> path.toString().compareTo(boundaryFileName) < 0).forEach(path -> {

                try {

                    Files.delete(path);
                    counterRef[0]++;
                }

                catch(final IOException exc) {

                    if(this.logger != null) this.logger.error("Could not delete", exc);
                    else System.err.println("Could not delete because: " + exc);
                }
            });

            return counterRef[0];
        }

        catch(final NoSuchFileException _) {

            return 0;
        }
    }

    ///..
    @Override
    public String toString() {

        return this.directoryPath.toString();
    }

    ///.
    private String composeName(final LocalDateTime dateTime) {

        // /path/filename_yyyy-MM-ddTHH.extension
        return fileNameComponents.get(0) + "_" + dateTime.toString().substring(0, 13) + "." + fileNameComponents.get(1);
    }

    ///..
    private StreamWriter openForWrite(final LocalDateTime window) throws IOException {

        Files.createDirectories(this.directoryPath);
        return new StreamWriter(Files.newOutputStream(Path.of(this.composeName(window)), StandardOpenOption.CREATE, StandardOpenOption.APPEND));
    }

    ///
}
