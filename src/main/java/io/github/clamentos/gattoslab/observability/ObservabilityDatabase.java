package io.github.clamentos.gattoslab.observability;

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
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

///
public final class ObservabilityDatabase {

    ///
    private final ObservabilityDirectory<LogEvent> logDirectory;
    private final ObservabilityDirectory<RequestMetricsEntity> requestMetricsDirectory;
    private final ObservabilityDirectory<SystemMetricsEntity> systemMetricsDirectory;

    ///
    public ObservabilityDatabase(

        final ObservabilityDirectory<LogEvent> logDirectory,
        final ObservabilityDirectory<RequestMetricsEntity> requestMetricsDirectory,
        final ObservabilityDirectory<SystemMetricsEntity> systemMetricsDirectory
    ) {

        this.logDirectory = logDirectory;
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

        return this.fetch(startTime, endTime, logDirectory, (line, start, end) -> {

            final List<String> log = GenericUtils.fastSplit(line, ApplicationProperties.FIELD_SEPARATOR);
            final long logTimestamp = Long.parseLong(log.get(1));

            final boolean isExcluded =

                (logTimestamp < start || logTimestamp > end) ||
                (!severity.isEmpty() && !log.get(2).equals(severity)) ||
                (!threadPattern.isEmpty() && !log.get(3).contains(threadPattern)) ||
                (!loggerPattern.isEmpty() && !log.get(4).contains(loggerPattern)) ||
                (!messagePattern.isEmpty() && !log.get(5).contains(messagePattern)) ||
                (!exceptionClassPattern.isEmpty() && !log.get(6).contains(exceptionClassPattern))
            ;

            return !isExcluded;
        });
    }

    ///..
    public List<String> readRequests(

        final long startTime,
        final long endTime,
        final String isUnknown,
        final String isUserAgentBlocked,
        final String userAgentPattern

    ) throws IOException, IllegalArgumentException {

        return this.fetch(startTime, endTime, requestMetricsDirectory, (line, start, end) -> {

            final List<String> request = GenericUtils.fastSplit(line, ApplicationProperties.FIELD_SEPARATOR);
            final long requestTimestamp = Long.parseLong(request.get(1));

            final boolean isExcluded =

                (requestTimestamp < start || requestTimestamp > end) ||
                (!isUnknown.isEmpty() && !isUnknown.equals(request.get(5))) ||
                (!isUserAgentBlocked.isEmpty() && !isUserAgentBlocked.equals(request.get(5))) ||
                (!userAgentPattern.isEmpty() && !request.get(4).contains(userAgentPattern))
            ;

            return !isExcluded;
        });
    }

    ///..
    public List<String> readSystemMetrics(final long startTime, final long endTime) throws IOException, IllegalArgumentException {

        return this.fetch(startTime, endTime, systemMetricsDirectory, (line, start, end) -> {

            final List<String> metrics = GenericUtils.fastSplit(line, ApplicationProperties.FIELD_SEPARATOR);
            final long requestTimestamp = Long.parseLong(metrics.get(1));

            return requestTimestamp >= start && requestTimestamp <= end;
        });
    }

    ///.
    private List<String> fetch(
        
        final long startTime,
        final long endTime,
        final ObservabilityDirectory<?> observabilityDirectory,
        final RecordFilter filter

    ) throws IOException, IllegalArgumentException {

        if(startTime > endTime) throw new IllegalArgumentException("'startTime' cannot be greater than 'endTime'");
        final List<String> filteredRecords = new ArrayList<>(256);

        final CharsetDecoder decoder = StandardCharsets.UTF_8

            .newDecoder()
            .onMalformedInput(CodingErrorAction.REPLACE)
            .onUnmappableCharacter(CodingErrorAction.REPLACE)
        ;

        try(final Stream<Path> files = Files.list(Path.of(observabilityDirectory.toString()))) {

            final List<Path> filteredFiles = this.filterFiles(files, startTime, endTime);
            final int length = filteredFiles.size();

            for(int i = 0; i < length; i++) {

                try(final BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(filteredFiles.get(i)), decoder))) {

                    String line;

                    while((line = reader.readLine()) != null) {

                        if(filter.apply(line, startTime, endTime)) filteredRecords.add(line);
                    }
                }

                decoder.reset();
            }
        }

        return filteredRecords;
    }

    ///..
    private List<Path> filterFiles(final Stream<Path> files, final long startTimeFilter, final long endTimeFilter) {

        final String fileStartTimeSegment = LocalDateTime

            .ofInstant(Instant.ofEpochMilli(startTimeFilter), GenericUtils.DEFAULT_ZONE_ID)
            .toString()
            .substring(0, 13)
        ;

        final String fileEndTimeSegment = LocalDateTime

            .ofInstant(Instant.ofEpochMilli(endTimeFilter), GenericUtils.DEFAULT_ZONE_ID)
            .toString()
            .substring(0, 13)
        ;

        return files.filter(path -> {

            String fileNameWithoutExtension = GenericUtils.fastSplit(path.getFileName().toString(), '.').get(0);
            final int length = fileNameWithoutExtension.length();

            fileNameWithoutExtension = fileNameWithoutExtension.substring(length - 13, length);
            return fileNameWithoutExtension.compareTo(fileStartTimeSegment) >= 0 && fileNameWithoutExtension.compareTo(fileEndTimeSegment) <= 0;
        })
        .sorted()
        .toList();
    }

    ///
}
