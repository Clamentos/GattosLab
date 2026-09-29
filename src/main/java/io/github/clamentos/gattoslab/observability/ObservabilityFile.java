package io.github.clamentos.gattoslab.observability;

///
import io.github.clamentos.gattoslab.exchange.handling.components.Streamable;
import io.github.clamentos.gattoslab.http.server.StreamWriter;
import io.github.clamentos.gattoslab.observability.logging.Logger;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

///
public final class ObservabilityFile<T extends Streamable> {

    ///
    private final Logger logger;

    ///..
    private final List<String> fileNameComponents;

    ///..
    private final AtomicReference<Path> filePath;
    private final AtomicReference<LocalDateTime> nextRolloverTimestamp;

    ///
    public ObservabilityFile(final Path filePath, final Logger logger) throws IOException {

        this.logger = logger;

        final String latestFileName = this.findLatestFile(filePath.getParent());
        final LocalDateTime nowDate = LocalDateTime.now(GenericUtils.DEFAULT_ZONE_ID);
        final String now = nowDate.toString().substring(0, 13);

        this.fileNameComponents = GenericUtils.fastSplit(filePath.toString(), '.');

        if(latestFileName != null) {

            final String latestFileNameNoType = GenericUtils.fastSplit(latestFileName, '.').get(0);
            final int length = latestFileNameNoType.length();

            if(latestFileNameNoType.substring(length - 13, length).equals(now)) this.filePath = new AtomicReference<>(Path.of(latestFileName));
            else this.filePath = new AtomicReference<>(Path.of(this.composeName(nowDate)));
        }

        else {

            this.filePath = new AtomicReference<>(Path.of(this.composeName(nowDate)));
        }

        nextRolloverTimestamp = new AtomicReference<>(nowDate.truncatedTo(ChronoUnit.HOURS).plusHours(1));
    }

    ///
    public void write(final List<T> entities) throws IOException {

        try(final StreamWriter writer = new StreamWriter(this.openFileAsAppend(this.filePath.get()))) {

            final int length = entities.size();

            for(int i = 0; i < length; i++) {

                entities.get(i).stream(writer);
                writer.write('\n');
            }
        }
    }

    ///..
    @SuppressWarnings("java:S106")
    public int deleteAll(final LocalDateTime boundaryDate) throws IOException {

        try(final Stream<Path> files = Files.list(Path.of(this.toString()))) {

            final String boundaryFileName = this.composeName(boundaryDate);
            final Path currentFile = this.filePath.get();
            final int[] counterRef = new int[]{0};

            files.filter(path -> !path.equals(currentFile) && path.toString().compareTo(boundaryFileName) < 0).forEach(path -> {

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

        return this.filePath.get().getParent().toString();
    }

    ///.
    private String findLatestFile(final Path parent) throws IOException {

        try(final Stream<Path> files = Files.list(parent)) {

            final Optional<String> latestFileMaybe = files.map(Path::toString).sorted((a, b) -> b.compareTo(a)).findFirst();

            if(latestFileMaybe.isPresent()) return latestFileMaybe.get();
            else return null;
        }

        catch(final NoSuchFileException _) {

            return null;
        }
    }

    ///..
    private String composeName(final LocalDateTime now) {

        // filename_yyyy-MM-ddTHH.extension
        return fileNameComponents.get(0) + "_" + now.toString().substring(0, 13) + "." + fileNameComponents.get(1);
    }

    ///..
    private OutputStream openFileAsAppend(final Path path) throws IOException {

        final Path parent = path.getParent();
        if(parent != null) Files.createDirectories(parent);

        final LocalDateTime now = LocalDateTime.now(GenericUtils.DEFAULT_ZONE_ID);
        final LocalDateTime nowHours = now.truncatedTo(ChronoUnit.HOURS);

        if(nowHours.equals(this.nextRolloverTimestamp.get())) {

            this.nextRolloverTimestamp.set(nowHours.plusHours(1));
            this.filePath.set(Path.of(this.composeName(now)));
        }

        return Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    ///
}
