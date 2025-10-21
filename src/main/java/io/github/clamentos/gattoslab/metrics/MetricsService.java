package io.github.clamentos.gattoslab.metrics;

///
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

///.
import io.github.clamentos.gattoslab.metrics.model.HitsTracker;
import io.github.clamentos.gattoslab.metrics.model.PerformanceEntry;
import io.github.clamentos.gattoslab.metrics.model.PerformanceMetrics;
import io.github.clamentos.gattoslab.metrics.model.PerformanceSubEntry;
import io.github.clamentos.gattoslab.utils.Pair;

///.
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

///.
import java.io.IOException;

///.
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

///.
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.context.support.ServletRequestHandledEvent;
import org.springframework.web.servlet.HandlerInterceptor;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

///
@Service
@Slf4j

///
public class MetricsService implements HandlerInterceptor {

    ///
    private final HitsTracker hitsTracker;
    private final PerformanceMetrics performanceMetrics;

    private final AtomicInteger index;

    ///..
    private final Set<String> paths;

    ///..
    private final List<Pair<Integer, Integer>> latencyBuckets;
    private final int rateCapacity;

    ///..
    private final Logger metricsLogger;
    private final ObjectMapper objectMapper;

    ///
    @Autowired
    public MetricsService(final Environment environment, final ObjectMapper objectMapper)
    throws IOException, ParserConfigurationException, SAXException {

        hitsTracker = new HitsTracker(

            environment.getProperty("app.metrics.maxTotalRequestCounterSize", Integer.class, 1000),
            environment.getProperty("app.metrics.maxUserAgentCounterSize", Integer.class, 250)
        );

        performanceMetrics = new PerformanceMetrics();

        index = new AtomicInteger();
        paths = new HashSet<>();

        final DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        final Document siteMap = builder.parse(new ClassPathResource("static/sitemap.xml").getInputStream());
        final NodeList nodeList = siteMap.getElementsByTagName("loc");

        for(int i = 0; i < nodeList.getLength(); i++) { //TODO: images too

            final String loc = nodeList.item(i).getTextContent();
            paths.add(nodeList.item(i).getTextContent().substring(loc.lastIndexOf('/')));
        }

        final String latencyBucketsString = environment.getProperty(

            "app.metrics.latencyBuckets",
            String.class,
            "1-10,11-20,21-50,51-100,101-200,201-500,501-1000"
        );

        latencyBuckets = Arrays
            .asList(latencyBucketsString.split(","))
            .stream()
            .map(element -> {

                final String[] pair = element.split("-");
                return new Pair<>(Integer.parseInt(pair[0]), Integer.parseInt(pair[1]));
            })
            .toList()
        ;

        rateCapacity = environment.getProperty("app.metrics.rateCapacity", Integer.class, 12);

        metricsLogger = LoggerFactory.getLogger("metrics");
        this.objectMapper = objectMapper;
    }

    ///
    public Map<String, Object> getHitsTrackerData() {

        final Map<String, Long> requestCountsMap = hitsTracker
            .getTotalRequestCounter()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, element -> element.getValue().get()))
        ;

        final Map<String, Long> userAgentCountsMap = hitsTracker
            .getUserAgentCounter()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey, element -> element.getValue().get()))
        ;

        final Map<String, Object> json = new LinkedHashMap<>();

        json.put("requestEntries", requestCountsMap.size());
        json.put("userAgentEntries", userAgentCountsMap.size());
        json.put("requests", requestCountsMap);
        json.put("userAgents", userAgentCountsMap);

        return json;
    }

    ///..
    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {

        hitsTracker.updateUserAgentCount(request.getHeader("User-Agent"));
        return true;
    }

    ///.
    @EventListener
    protected void handleRequestHandledEvent(final ServletRequestHandledEvent requestHandledEvent) {

        final String trueUrl = requestHandledEvent.getRequestUrl();
        final String url = paths.contains(trueUrl) ? trueUrl : "<other>";

        hitsTracker.updateRequestCount(trueUrl);

        performanceMetrics
            .getMetricsMap()
            .computeIfAbsent(url, _ -> new PerformanceEntry(rateCapacity, latencyBuckets))
            .update(

                HttpStatus.valueOf(requestHandledEvent.getStatusCode()),
                (int)requestHandledEvent.getProcessingTimeMillis(),
                index.get()
            );
    }

    ///.
    @Scheduled(fixedRateString = "${app.metrics.logDelay}")
    protected void scheduledMetricsTask() {

        index.incrementAndGet();

        if(index.get() == rateCapacity) {

            final Map<String, PerformanceEntry> oldMap = performanceMetrics.swap();
            index.set(0);

            final String message = this.createMetricsLogEntry(oldMap);

            if(message != null && !message.equals("")) metricsLogger.info(message);
            oldMap.clear();
        }
    }

    ///.
    private String createMetricsLogEntry(final Map<String, PerformanceEntry> metrics) {

        final Map<String, Object> metricsLog = new LinkedHashMap<>();

        for(final Map.Entry<String, PerformanceEntry> entry : metrics.entrySet()) {

            final Map<String, Object> metricsLogEntry = new LinkedHashMap<>();

            this.createLogEntry(metricsLogEntry, entry.getValue());
            metricsLog.put(entry.getKey(), metricsLogEntry);
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
    private void createLogEntry(final Map<String, Object> metricsLog, final PerformanceEntry metricsEntry) {

        final Map<String, List<Long>> rpsLog = new LinkedHashMap<>();
        final Map<String, Map<String, Long>> responseTimes = new LinkedHashMap<>();

        for(final Map.Entry<HttpStatus, PerformanceSubEntry> entry : metricsEntry.getSubEntries().entrySet()) {

            final String key = entry.getKey().getReasonPhrase();
            final PerformanceSubEntry value = entry.getValue();

            rpsLog.put(key, value.getRequestCounter().stream().map(AtomicLong::get).toList());

            responseTimes.put(key, value.getResponseTimeDistribution().stream().collect(Collectors.toMap(

                element -> element.getStart() + "-" + element.getEnd(),
                element -> element.getCount().get()
            )));
        }

        metricsLog.put("rps", rpsLog);
        metricsLog.put("responseTimes", responseTimes);
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

        "responseTimes": {

            "OK": {"0-99": 1},
            "NOT_FOUND": {"100-199": 1}
        }
    },

    ...
   }
*/
