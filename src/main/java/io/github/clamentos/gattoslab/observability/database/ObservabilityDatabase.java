package io.github.clamentos.gattoslab.observability.database;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.observability.logging.entities.LogEvent;
import io.github.clamentos.gattoslab.observability.metrics.entities.RequestMetricsEntity;
import io.github.clamentos.gattoslab.observability.metrics.entities.SystemMetricsEntity;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

///
public final class ObservabilityDatabase {

    ///
    private final ObservabilityDirectory<LogEvent> logsDirectory;
    private final ObservabilityDirectory<RequestMetricsEntity> requestMetricsDirectory;
    private final ObservabilityDirectory<SystemMetricsEntity> systemMetricsDirectory;

    ///
    public ObservabilityDatabase(

        final ObservabilityDirectory<LogEvent> logsDirectory,
        final ObservabilityDirectory<RequestMetricsEntity> requestMetricsDirectory,
        final ObservabilityDirectory<SystemMetricsEntity> systemMetricsDirectory
    ) {

        this.logsDirectory = logsDirectory;
        this.requestMetricsDirectory = requestMetricsDirectory;
        this.systemMetricsDirectory = systemMetricsDirectory;
    }

    ///
    public List<String> readLogs(

        final long startTime,
        final long endTime,
        final String severity,
        final String threadPattern,
        final String loggerPattern,
        final String messagePattern,
        final String exceptionClassPattern

    ) throws IOException, IllegalArgumentException {

        return this.fetch(startTime, endTime, logsDirectory, line -> {

            final List<String> log = GenericUtils.fastSplit(line, ApplicationProperties.FIELD_SEPARATOR);
            final long logTimestamp = Long.parseLong(log.get(0));

            final boolean isExcluded =

                (logTimestamp < startTime || logTimestamp > endTime) ||
                (!severity.isEmpty() && !log.get(1).equals(severity)) ||
                (!threadPattern.isEmpty() && !log.get(2).contains(threadPattern)) ||
                (!loggerPattern.isEmpty() && !log.get(3).contains(loggerPattern)) ||
                (!messagePattern.isEmpty() && !log.get(4).contains(messagePattern)) ||
                (!exceptionClassPattern.isEmpty() && !log.get(5).contains(exceptionClassPattern))
            ;

            return !isExcluded;
        });
    }

    ///..
    public List<String> readRequests(

        final long startTime,
        final long endTime,
        final String isUnknown,
        final String userAgentPattern,
        final String uriPattern,
        final String domainPattern,
        final String remoteAddressPattern

    ) throws IOException, IllegalArgumentException {

        return this.fetch(startTime, endTime, requestMetricsDirectory, line -> {

            final List<String> request = GenericUtils.fastSplit(line, ApplicationProperties.FIELD_SEPARATOR);
            final long requestTimestamp = Long.parseLong(request.get(0));

            final boolean isExcluded =

                (requestTimestamp < startTime || requestTimestamp > endTime) ||
                (!isUnknown.isEmpty() && !isUnknown.equals(request.get(5))) ||
                (!userAgentPattern.isEmpty() && !request.get(4).contains(userAgentPattern)) ||
                (!uriPattern.isEmpty() && !request.get(3).contains(uriPattern)) ||
                (!domainPattern.isEmpty() && !request.get(2).contains(domainPattern)) ||
                (!remoteAddressPattern.isEmpty() && !request.get(8).contains(remoteAddressPattern))
            ;

            return !isExcluded;
        });
    }

    ///..
    public List<String> readSystemMetrics(final long startTime, final long endTime) throws IOException, IllegalArgumentException {

        return this.fetch(startTime, endTime, systemMetricsDirectory, line -> {

            final List<String> metrics = GenericUtils.fastSplit(line, ApplicationProperties.FIELD_SEPARATOR);
            final long requestTimestamp = Long.parseLong(metrics.get(0));

            return requestTimestamp >= startTime && requestTimestamp <= endTime;
        });
    }

    ///.
    private List<String> fetch(

        final long startTime,
        final long endTime,
        final ObservabilityDirectory<?> observabilityDirectory,
        final Predicate<String> filter

    ) throws IOException, IllegalArgumentException {

        if(startTime > endTime) throw new IllegalArgumentException("'startTime' cannot be greater than 'endTime'");
        final List<String> filteredRecords = new ArrayList<>(256);

        final CharsetDecoder decoder = StandardCharsets.ISO_8859_1

            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
        ;

        try(final Stream<Path> files = Files.list(observabilityDirectory.getDirectory())) {

            final List<Path> filteredFiles = this.filterFiles(observabilityDirectory, files, startTime, endTime);
            final int length = filteredFiles.size();

            for(int i = 0; i < length; i++) {

                try(final BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(filteredFiles.get(i)), decoder))) {

                    String line;

                    while((line = reader.readLine()) != null) {

                        if(filter.test(line)) filteredRecords.add(line);
                    }
                }

                decoder.reset();
            }
        }

        catch(final NoSuchFileException _) {

            return new ArrayList<>(1);
        }

        return filteredRecords;
    }

    ///..
    private List<Path> filterFiles(

        final ObservabilityDirectory<?> observabilityDirectory,
        final Stream<Path> files,
        final long startTimeFilter,
        final long endTimeFilter
    ) {

        final long startTimeFilterClamped = Math.max(startTimeFilter, GenericUtils.MIN_ISO_DATE_TIMESTAMP);
        final long endTimeFilterClamped = Math.min(endTimeFilter, GenericUtils.MAX_ISO_DATE_TIMESTAMP);

        final String fileStart = observabilityDirectory.composePath(LocalDateTime.ofInstant(

            Instant.ofEpochMilli(startTimeFilterClamped),
            GenericUtils.DEFAULT_ZONE_ID

        )).getFileName().toString();

        final String fileEnd = observabilityDirectory.composePath(LocalDateTime.ofInstant(

            Instant.ofEpochMilli(endTimeFilterClamped),
            GenericUtils.DEFAULT_ZONE_ID

        )).getFileName().toString();

        return files.filter(path -> {

            final String pathString = path.getFileName().toString();
            return pathString.compareTo(fileStart) >= 0 && pathString.compareTo(fileEnd) <= 0;
        })
        .sorted()
        .toList();
    }

    ///
}
