package io.github.clamentos.gattoslab.observability;

///
import com.mongodb.MongoException;

///.
import io.github.clamentos.gattoslab.observability.filters.TemporalSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.RequestMetricsSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.LogSearchFilter;
import io.github.clamentos.gattoslab.observability.logging.LogsService;
import io.github.clamentos.gattoslab.session.SessionMetadata;
import io.github.clamentos.gattoslab.session.SessionService;
import io.github.clamentos.gattoslab.utils.MutableLong;

///.
import java.util.List;
import java.util.Map;

///.
import lombok.RequiredArgsConstructor;

///.
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

///
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/api/observability")

///
public final class ObservabilityController {

    ///
    private final ObservabilityService observabilityService;
    private final SessionService sessionService;
    private final LogsService logsService;

    ///
    @PostMapping(path = "/paths-invocations", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, MutableLong>> getPathInvocations(@RequestBody final TemporalSearchFilter searchFilter) throws MongoException {

        return ResponseEntity.ok(observabilityService.getPathInvocations(searchFilter));
    }

    ///..
    @PostMapping(path = "/user-agents-count", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, MutableLong>> getUserAgentsCount(@RequestBody final TemporalSearchFilter searchFilter) throws MongoException {

        return ResponseEntity.ok(observabilityService.getUserAgentsCount(searchFilter));
    }

    ///..
    @PostMapping(path = "/request-metrics", consumes = "application/json", produces = "application/json")
    public ResponseEntity<StreamingResponseBody> getRequestMetrics(@RequestBody final RequestMetricsSearchFilter chartSearchFilter)
    throws MongoException {

        return ResponseEntity.ok().header("Content-Encoding", "gzip").body(observabilityService.getRequestMetrics(chartSearchFilter));
    }

    ///..
    @PostMapping(path = "/system-metrics", produces = "application/json")
    public ResponseEntity<StreamingResponseBody> getSystemMetrics(@RequestBody final TemporalSearchFilter searchFilter) throws MongoException {

        return ResponseEntity.ok().header("Content-Encoding", "gzip").body(observabilityService.getSystemMetrics(searchFilter));
    }

    ///..
    @GetMapping(path = "/sessions-metadata", produces = "application/json")
    public ResponseEntity<List<SessionMetadata>> getSessionsMetadata() {

        return ResponseEntity.ok(sessionService.getSessionsMetadata());
    }

    ///..
    @PostMapping(path = "/logs", consumes = "application/json", produces = "application/json")
    public ResponseEntity<StreamingResponseBody> getLogs(@RequestBody final LogSearchFilter logSearchFilter) throws MongoException {

        return ResponseEntity.ok().header("Content-Encoding", "gzip").body(logsService.getLogs(logSearchFilter));
    }

    ///..
    @GetMapping(path = "/fallback-logs", produces = "application/json")
    public ResponseEntity<StreamingResponseBody> getFallbackLogs() {

        return ResponseEntity.ok().header("Content-Encoding", "gzip").body(logsService.getFallbackLogs());
    }

    ///
}
