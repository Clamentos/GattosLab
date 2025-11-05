package io.github.clamentos.gattoslab.metrics;

///
import io.github.clamentos.gattoslab.admin.AdminSessionMetadata;
import io.github.clamentos.gattoslab.admin.AdminSessionService;
import io.github.clamentos.gattoslab.exceptions.RuntimeIOException;

///.
import java.util.Collection;
import java.util.Set;

///.
import lombok.RequiredArgsConstructor;

///.
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

///
@RequiredArgsConstructor
@RestController
@RequestMapping("/admin/api/metrics")

///
public final class MetricsController {

    ///
    private final MetricsService metricsService;
    private final AdminSessionService adminSessionService;

    ///
    @GetMapping("/paths-count")
    public ResponseEntity<StreamingResponseBody> getPathsCount() {

        return ResponseEntity.ok(metricsService.getPathsCount());
    }

    ///..
    @GetMapping("/user-agents-count")
    public ResponseEntity<StreamingResponseBody> getUserAgentsCount() {

        return ResponseEntity.ok(metricsService.getUserAgentsCount());
    }

    ///..
    @GetMapping("/sessions-metadata")
    public ResponseEntity<Collection<AdminSessionMetadata>> getSessionsMetadata() {

        return ResponseEntity.ok(adminSessionService.getSessionsMetadata());
    }

    ///..
    @GetMapping("/performance-metrics")
    public ResponseEntity<StreamingResponseBody> getMetrics(

        @RequestParam final long startTimestamp,
        @RequestParam final long endTimestamp

    ) throws RuntimeIOException {

        return ResponseEntity.ok().body(metricsService.getPerformanceMetrics(startTimestamp, endTimestamp));
    }

    ///..
    @GetMapping("/logs")
    public ResponseEntity<StreamingResponseBody> getLogs(

        @RequestParam final long startTimestamp,
        @RequestParam final long endTimestamp,
        @RequestParam final Set<String> logLevels

    ) throws RuntimeIOException {

        return ResponseEntity.ok().body(metricsService.getLogs(startTimestamp, endTimestamp, logLevels));
    }

    ///
}
