package io.github.clamentos.gattoslab.metrics;

///.
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

///.
import io.github.clamentos.gattoslab.exceptions.RuntimeIOException;
import io.github.clamentos.gattoslab.metrics.model.PerformanceMetrics;
import io.github.clamentos.gattoslab.metrics.model.PerformanceMetricsEntry;
import io.github.clamentos.gattoslab.metrics.model.RequestTracker;
import io.github.clamentos.gattoslab.utils.Pair;
import io.github.clamentos.gattoslab.utils.PropertyProvider;
import io.github.clamentos.gattoslab.web.StaticSite;

///.
import jakarta.el.PropertyNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

///.
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.ServletRequestHandledEvent;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

///
@Service
@Slf4j

///
public final class MetricsService implements HandlerInterceptor {

    ///
    private final RequestTracker requestsTracker;
    private final PerformanceMetrics performanceMetrics;
    private final AtomicInteger timeSlotIndex;

    ///..
    private final Logger metricsLogger;
    private final MetricsLogMapper metricsLogMapper;

    ///..
    private final Set<String> sitePaths;
    private final List<Pair<Integer, Integer>> latencyBuckets;
    private final int rateCapacity;
    private final ZoneId currentZone;
    private final DateTimeFormatter dateTimeFormatter;

    ///
    @Autowired
    public MetricsService(final StaticSite staticSite, final MetricsLogMapper metricsLogMapper, final PropertyProvider propertyProvider) 
    throws PropertyNotFoundException {

        requestsTracker = new RequestTracker(

            propertyProvider.getProperty("app.metrics.maxTotalRequestCounterSize", Integer.class),
            propertyProvider.getProperty("app.metrics.maxUserAgentCounterSize", Integer.class)
        );

        performanceMetrics = new PerformanceMetrics();
        timeSlotIndex = new AtomicInteger();

        sitePaths = staticSite.getSitePaths().stream().map(e -> "GET" + e).collect(Collectors.toCollection(HashSet::new));
        sitePaths.add("GET/admin/api/metrics/paths-count");
        sitePaths.add("GET/admin/api/metrics/user-agents-count");
        sitePaths.add("GET/admin/api/metrics/performance-metrics");
        sitePaths.add("GET/admin/api/metrics/sessions-metadata");
        sitePaths.add("GET/admin/api/logs");
        sitePaths.add("POST/admin/api/session");
        sitePaths.add("DELETE/admin/api/session");

        latencyBuckets = Arrays

            .asList(propertyProvider.getProperty("app.metrics.latencyBuckets", String.class).split(","))
            .stream()
            .map(element -> {

                final String[] pair = element.split("-");
                return new Pair<>(Integer.parseInt(pair[0]), Integer.parseInt(pair[1]));
            })
            .toList()
        ;

        rateCapacity = propertyProvider.getProperty("app.metrics.rateCapacity", Integer.class);
        metricsLogger = LoggerFactory.getLogger("metrics");
        this.metricsLogMapper = metricsLogMapper;
        currentZone = ZoneId.systemDefault();
        dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS");
    }

    ///
    public StreamingResponseBody getPathsCount() {

        return outputStream -> this.writeRequestTracker(requestsTracker.getPathCounters(), outputStream, "path");
    }

    ///..
    public StreamingResponseBody getUserAgentsCount() {

        return outputStream -> this.writeRequestTracker(requestsTracker.getUserAgentCounters(), outputStream, "userAgent");
    }

    ///..
    public StreamingResponseBody getPerformanceMetrics(final long startTimestamp, final long endTimestamp) throws RuntimeIOException {

        return outputStream -> {

            final LocalDateTime startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTimestamp), currentZone);
            final LocalDateTime endTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTimestamp), currentZone);

            try(

                final Stream<Path> metricsFiles = Files.list(Path.of("./metrics"));
                final JsonGenerator generator = new JsonFactory(metricsLogMapper.getObjectMapper()).createGenerator(outputStream)
            ) {

                generator.writeStartArray();

                metricsFiles.forEach(file -> {

                    final String path = file.toString();
                    final int hookIdx = path.lastIndexOf("app_metrics.") + 12;
                    final String dateString = path.substring(hookIdx, hookIdx + 10);
                    final LocalDateTime date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();

                    if(date.compareTo(startTime) >= 0 && date.compareTo(endTime) <= 0) {

                        try(final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file.toFile())))) {

                            String line;

                            while((line = in.readLine()) != null) {

                                final String[] splits = line.split("\\|");

                                if(splits.length == 3) {

                                    final LocalDateTime lineDate = LocalDateTime.parse(splits[1], dateTimeFormatter);

                                    if(lineDate.compareTo(startTime) >= 0 && lineDate.compareTo(endTime) <= 0) {

                                        final long timestamp = ZonedDateTime.of(lineDate, currentZone).toInstant().toEpochMilli();

                                        generator.writeStartObject();
                                        generator.writeNumberField("timestamp", timestamp);
                                        generator.writeStringField("data", splits[2]);
                                        generator.writeEndObject();
                                    }
                                }

                                else {

                                    generator.writeNull();
                                }
                            }
                        }

                        catch(final IOException exc) {

                            log.error("Could not process metrics file", exc);
                            throw new RuntimeIOException(exc);
                        }
                    }
                });

                generator.writeEndArray();
            }
        };
    }

    ///..
    public StreamingResponseBody getLogs(final long startTimestamp, final long endTimestamp, final Set<String> logLevels)
    throws RuntimeIOException {

        return outputStream -> {

            final LocalDateTime startTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(startTimestamp), currentZone);
            final LocalDateTime endTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTimestamp), currentZone);

            try(final Stream<Path> logFiles = Files.list(Path.of("./logs"))) {

                logFiles.forEach(file -> {

                    final String path = file.toString();
                    final int hookIdx = path.lastIndexOf("app_logs.") + 9;
                    final String dateString = path.substring(hookIdx, hookIdx + 10);
                    final LocalDateTime date = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();

                    if(date.compareTo(startTime) >= 0 && date.compareTo(endTime) <= 0) {

                        try(final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file.toFile())))) {

                            String line;

                            while((line = in.readLine()) != null) {

                                final String[] splits = line.split("\\|");
                                final LocalDateTime lineDate = LocalDateTime.parse(splits[1], dateTimeFormatter);

                                if(
                                    lineDate.compareTo(startTime) >= 0 && lineDate.compareTo(endTime) <= 0 &&
                                    logLevels.contains(splits[2])
                                ) {
                                    outputStream.write(line.getBytes());
                                } 
                            }
                        }

                        catch(final IOException exc) {

                            log.error("Could not process log file", exc);
                            throw new RuntimeIOException(exc);
                        }
                    }
                });
            }
        };
    }

    ///..
    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {

        final String userAgent = request.getHeader("User-Agent");
        requestsTracker.updateUserAgentCount(userAgent != null ? userAgent : "null");

        return true;
    }

    ///.
    @EventListener
    protected void handleRequestHandledEvent(final ServletRequestHandledEvent requestHandledEvent) {

        final String trueUrl = requestHandledEvent.getMethod() + requestHandledEvent.getRequestUrl();
        final String url = sitePaths.contains(trueUrl) ? trueUrl : "<other>";
        final HttpStatus status = HttpStatus.valueOf(requestHandledEvent.getStatusCode());
        final int processingTime = (int)requestHandledEvent.getProcessingTimeMillis();

        requestsTracker.updateRequestCount(trueUrl);

        performanceMetrics

            .getMetricsMap()
            .computeIfAbsent(status, _ -> new PerformanceMetricsEntry(rateCapacity, latencyBuckets))
            .update(url, processingTime, timeSlotIndex.get())
        ;
    }

    ///..
    @Scheduled(fixedRateString = "${app.metrics.logSchedule}")
    protected void scheduledMetricsTask() {

        timeSlotIndex.incrementAndGet();

        if(timeSlotIndex.get() == rateCapacity) {

            timeSlotIndex.set(0);

            final Map<HttpStatus, PerformanceMetricsEntry> oldMap = performanceMetrics.swap();
            final String message = metricsLogMapper.createMetricsLogEntry(oldMap);

            if(message != null && !message.equals("")) metricsLogger.info(message);
            oldMap.clear();
        }
    }

    ///.
    private void writeRequestTracker(final Map<String, AtomicLong> tracker, final OutputStream outputStream, final String name)
    throws IOException {

        try(JsonGenerator generator = new JsonFactory(metricsLogMapper.getObjectMapper()).createGenerator(outputStream)) {

            generator.writeStartArray();

            for(final Map.Entry<String, AtomicLong> entry : tracker.entrySet()) {

                generator.writeStartObject();
                generator.writeStringField(name, entry.getKey());
                generator.writeNumberField("count", entry.getValue().get());
                generator.writeEndObject();
            }

            generator.writeEndArray();
        }
    }

    ///
}
