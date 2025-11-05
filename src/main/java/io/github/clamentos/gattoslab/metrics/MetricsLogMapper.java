package io.github.clamentos.gattoslab.metrics;

///
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

///.
import io.github.clamentos.gattoslab.metrics.model.PerformanceMetricsEntry;
import io.github.clamentos.gattoslab.metrics.model.PerformanceMetricsSubEntry;

///.
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

///.
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

///
@RequiredArgsConstructor
@Getter
@Service
@Slf4j

///
public final class MetricsLogMapper {

    ///
    private final ObjectMapper objectMapper;

    ///
    public String createMetricsLogEntry(final Map<HttpStatus, PerformanceMetricsEntry> metrics) {

        final Map<HttpStatus, Object> metricsLog = new LinkedHashMap<>();

        for(final Map.Entry<HttpStatus, PerformanceMetricsEntry> metricsEntry : metrics.entrySet()) {

            final Map<String, Object> metricsLogEntry = new LinkedHashMap<>();

            this.populateLogEntry(metricsLogEntry, metricsEntry.getValue());
            metricsLog.put(metricsEntry.getKey(), metricsLogEntry);
        }

        if(!metricsLog.isEmpty()) {

            try {

                return objectMapper.writeValueAsString(metricsLog);
            }

            catch(final JsonProcessingException exc) {

                log.error("Could not serialize metrics", exc);
                return "Check logs";
            }
        }

        return "";
    }

    ///.
    private void populateLogEntry(final Map<String, Object> metricsLog, final PerformanceMetricsEntry metricsEntry) {

        final Map<String, List<Long>> rpsLog = new LinkedHashMap<>();
        final Map<String, Map<String, Long>> responseTimes = new LinkedHashMap<>();

        for(final Map.Entry<String, PerformanceMetricsSubEntry> subEntry : metricsEntry.getSubEntries().entrySet()) {

            final String key = subEntry.getKey();
            final PerformanceMetricsSubEntry value = subEntry.getValue();

            rpsLog.put(key, value.getRequestCounters().stream().map(AtomicLong::get).toList());

            responseTimes.put(key, value.getResponseTimeDistribution().stream().collect(Collectors.toMap(

                element -> element.getStart() + "-" + element.getEnd(),
                element -> element.getCount().get()
            )));
        }

        metricsLog.put("requestsPerSecond", rpsLog);
        metricsLog.put("latencyDistributions", responseTimes);
    }

    ///
}
