package io.github.clamentos.gattoslab.metrics;

///
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

///.
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.ServletRequestHandledEvent;

///
@Service
@Slf4j

///
public class MetricsService {

    ///
    private final Map<String, AtomicLong> totalRequests; // Since last reboot.
    private final Map<String, MetricsEntry> metricsFront;
    private final Map<String, MetricsEntry> metricsBack;

    private final AtomicInteger index;
    private final AtomicBoolean frontOrBack;

    ///..
    private final int numBuckets;
    private final int maxLatencyBucket;
    private final int rateCapacity;

    ///..
    private final Logger metricsLogger;

    ///..
    private final ObjectMapper objectMapper;

    ///
    @Autowired
    public MetricsService(final Environment environment, final ObjectMapper objectMapper) {

        totalRequests = new ConcurrentHashMap<>();
        metricsFront = new ConcurrentHashMap<>();
        metricsBack = new ConcurrentHashMap<>();

        index = new AtomicInteger();
        frontOrBack = new AtomicBoolean(true);

        numBuckets = environment.getProperty("app.metrics.numBuckets", Integer.class, 8);
        maxLatencyBucket = environment.getProperty("app.metrics.maxLatencyBucket", Integer.class, 1000);
        rateCapacity = environment.getProperty("app.metrics.rateCapacity", Integer.class, 12);

        metricsLogger = LoggerFactory.getLogger("metrics");
        this.objectMapper = objectMapper;
    }

    ///
    public Map<String, Long> getTotalRequests() {

        return totalRequests.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, element -> element.getValue().get()));
    }

    ///.
    @EventListener
    protected void handleRequestHandledEvent(final ServletRequestHandledEvent requestHandledEvent) {

        final String url = requestHandledEvent.getRequestUrl();
        final HttpStatus status = HttpStatus.valueOf(requestHandledEvent.getStatusCode());
        final int elapsedTime = (int)requestHandledEvent.getProcessingTimeMillis();
        final Map<String, MetricsEntry> metrics = frontOrBack.get() ? metricsFront : metricsBack;

        totalRequests.computeIfAbsent(url, _ -> new AtomicLong()).incrementAndGet();

        metrics
            .computeIfAbsent(url, _ -> new MetricsEntry(rateCapacity, numBuckets, maxLatencyBucket))
            .update(status, elapsedTime, index.get());
    }

    ///..
    @Scheduled(fixedRateString = "${app.metrics.logDelay}")
    protected void scheduledMetricsTask() {

        index.incrementAndGet();

        if(index.get() == rateCapacity) {

            final boolean frontOrBackValue = frontOrBack.get();
            final Map<String, MetricsEntry> metrics = frontOrBackValue ? metricsFront : metricsBack;

            frontOrBack.set(!frontOrBackValue);
            index.set(0);

            final String message = this.createMetricsLogEntry(metrics);

            if(message != null && !message.equals("")) metricsLogger.info(message);
            metrics.clear();
        }
    }

    ///.
    private String createMetricsLogEntry(final Map<String, MetricsEntry> metrics) {

        final Map<String, Object> metricsLog = new LinkedHashMap<>();

        for(final Map.Entry<String, MetricsEntry> entry : metrics.entrySet()) {

            final String url = entry.getKey();
            final MetricsEntry metricsEntry = entry.getValue();
            final Map<String, Object> metricsLogEntry = new LinkedHashMap<>();

            metricsLogEntry.put("rps", this.createRpsLogEntry(metricsEntry));
            metricsLogEntry.put("latencies", this.createLatencyLogEntry(metricsEntry));
            metricsLog.put(url, metricsLogEntry);
        }

        if(!metricsLog.isEmpty()) {

            try {

                return objectMapper.writeValueAsString(metricsLog);
            }

            catch(final JsonProcessingException exc) {

                log.error("Could not serialize metrics", exc);
                return "Check error logs";
            }
        }

        return "";
    }

    ///..
    private Map<String, Object> createRpsLogEntry(final MetricsEntry metricsEntry) {

        final Map<String, Object> rpsLog = new LinkedHashMap<>();

        for(final Map.Entry<HttpStatus, List<AtomicInteger>> entry : metricsEntry.getRequestCounts().entrySet()) {

            final List<Integer> latencies = entry.getValue().stream().map(AtomicInteger::get).toList();
            rpsLog.put(entry.getKey().getReasonPhrase(), latencies);
        }

        return rpsLog;
    }

    ///..
    private Map<String, Object> createLatencyLogEntry(final MetricsEntry metricsEntry) {

        final Map<String, Object> latencyLog = new LinkedHashMap<>();

        for(final Map.Entry<HttpStatus, List<LatencyRange>> entry : metricsEntry.getLatencies().entrySet()) {

            final Map<String, Integer> latencies = entry.getValue().stream().collect(Collectors.toMap(

                element -> element.getStart() + "-" + element.getEnd(),
                element -> element.getCount().get()
            ));

            latencyLog.put(entry.getKey().getReasonPhrase(), latencies);
        }

        return latencyLog;
    }

    ///
}

/*
 * SCHEMA
 * 
 * {
    "/index.html": {

        "rps": {

            "OK": [0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0],
            "NOT_FOUND": [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        },

        "latencies": {

            "OK": {"0-99": 1},
            "NOT_FOUND": {"100-199": 1}
        }
    },

    ...
   }
*/
