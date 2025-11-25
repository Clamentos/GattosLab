package io.github.clamentos.gattoslab.observability;

///
import com.mongodb.MongoException;

///.
import io.github.clamentos.gattoslab.admin.AdminSessionMetadata;
import io.github.clamentos.gattoslab.admin.AdminSessionService;
import io.github.clamentos.gattoslab.observability.filters.TemporalSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.ChartSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.LogSearchFilter;
import io.github.clamentos.gattoslab.observability.logging.LogsService;
import io.github.clamentos.gattoslab.observability.metrics.system.SystemStatus;
import io.github.clamentos.gattoslab.utils.MutableLong;

///.
import java.util.Collection;
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
    private final AdminSessionService adminSessionService;
    private final LogsService logsService;

    ///
    @PostMapping(path = "/paths-count", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, MutableLong>> getPathsCount(@RequestBody final TemporalSearchFilter searchFilter) throws MongoException {

        return ResponseEntity.ok(observabilityService.getPathsCount(searchFilter));
    }

    ///..
    @PostMapping(path = "/user-agents-count", consumes = "application/json", produces = "application/json")
    public ResponseEntity<Map<String, MutableLong>> getUserAgentsCount(@RequestBody final TemporalSearchFilter searchFilter) throws MongoException {

        return ResponseEntity.ok(observabilityService.getUserAgentsCount(searchFilter));
    }

    ///..
    @PostMapping(path = "/performance-charts", consumes = "application/json", produces = "application/json")
    public ResponseEntity<StreamingResponseBody> getCharts(@RequestBody final ChartSearchFilter chartSearchFilter) throws MongoException {

        return ResponseEntity.ok().body(observabilityService.getCharts(chartSearchFilter));
    }

    ///..
    @GetMapping(path = "/jvm-metrics", produces = "application/json")
    public ResponseEntity<SystemStatus> getJvmMetrics() {

        return ResponseEntity.ok().body(observabilityService.getJvmMetrics());
    }

    ///..
    @GetMapping(path = "/sessions-metadata", produces = "application/json")
    public ResponseEntity<Collection<AdminSessionMetadata>> getSessionsMetadata() {

        return ResponseEntity.ok(adminSessionService.getSessionsMetadata());
    }

    ///..
    @PostMapping(path = "/logs", consumes = "application/json", produces = "application/json")
    public ResponseEntity<StreamingResponseBody> getLogs(@RequestBody final LogSearchFilter logSearchFilter) throws MongoException {

        return ResponseEntity.ok().body(logsService.getLogs(logSearchFilter));
    }

    ///
}
