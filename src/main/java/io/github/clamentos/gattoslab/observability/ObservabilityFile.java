package io.github.clamentos.gattoslab.observability;

///
import io.github.clamentos.gattoslab.datastructures.FastAsciiJoiner;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.BufferedWriter;
import java.io.IOException;
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

///..
import lombok.Getter;

///
public final class ObservabilityFile<T extends Printable> {

    ///
    @Getter
    private final int joinerSpacePerEntity;

    ///..
    private final List<String> fileNameComponents;

    ///..
    private final AtomicReference<Path> filePath;
    private final AtomicReference<LocalDateTime> nextRolloverTimestamp;

    ///
    public ObservabilityFile(final String filePath, final int joinerSpacePerEntity) throws IOException {

        this.joinerSpacePerEntity = joinerSpacePerEntity;

        final String latestFileName = this.findLatestFile(Path.of(filePath).getParent());
        final LocalDateTime nowDate = LocalDateTime.now(GenericUtils.DEFAULT_ZONE_ID);
        final String now = nowDate.toString().substring(0, 13);

        this.fileNameComponents = GenericUtils.fastSplit(filePath, '.');

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

        int failureCounter = 0;
        IOException lastFailure = null;

        try(final BufferedWriter writer = this.openFileAsAppend(this.filePath.get())) {

            final int length = entities.size();
            final FastAsciiJoiner joiner = new FastAsciiJoiner(length * (this.joinerSpacePerEntity + 1));

            for(int i = 0; i < length; i++) {

                entities.get(i).appendBytes(joiner);
                joiner.add("\n");
            }

            try {

                writer.write(joiner.toCharArray());
            }

            catch(final IOException exc) {

                failureCounter++;
                lastFailure = exc;
            }
        }

        if(failureCounter > 0) throw new IOException(failureCounter + " errors because", lastFailure);
    }

    ///..
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

                    System.err.println("Could not delete because: " + exc);
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
    private BufferedWriter openFileAsAppend(final Path path) throws IOException {

        final Path parent = path.getParent();
        if(parent != null) Files.createDirectories(parent);

        final LocalDateTime now = LocalDateTime.now(GenericUtils.DEFAULT_ZONE_ID);
        final LocalDateTime nowHours = now.truncatedTo(ChronoUnit.HOURS);

        if(nowHours.equals(this.nextRolloverTimestamp.get())) {

            this.nextRolloverTimestamp.set(nowHours.plusHours(1));
            this.filePath.set(Path.of(this.composeName(now)));
        }

        return Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    ///
}
