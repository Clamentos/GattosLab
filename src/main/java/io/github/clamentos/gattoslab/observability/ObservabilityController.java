package io.github.clamentos.gattoslab.observability;

///
import com.mongodb.MongoException;

///.
import io.github.clamentos.gattoslab.observability.filters.LogSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.RequestMetricsSearchFilter;
import io.github.clamentos.gattoslab.observability.filters.TemporalSearchFilter;
import io.github.clamentos.gattoslab.observability.logging.LogsService;
import io.github.clamentos.gattoslab.session.SessionMetadata;
import io.github.clamentos.gattoslab.session.SessionService;

///.
import java.util.List;

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

///..
import org.jspecify.annotations.NonNull;

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
    @PostMapping(path = "/request-metrics", consumes = "application/json", produces = "application/json")
    public @NonNull ResponseEntity<StreamingResponseBody> getRequestMetrics(@RequestBody @NonNull final RequestMetricsSearchFilter chartSearchFilter)
    throws MongoException {

        return ResponseEntity

            .ok()
            .header("Content-Encoding", "gzip")
            .header("Cache-Control", "no-cache")
            .body(observabilityService.getRequestMetrics(chartSearchFilter))
        ;
    }

    ///..
    @PostMapping(path = "/system-metrics", produces = "application/json")
    public @NonNull ResponseEntity<StreamingResponseBody> getSystemMetrics(@RequestBody(required = true) @NonNull final TemporalSearchFilter searchFilter)
    throws MongoException {

        return ResponseEntity

            .ok()
            .header("Content-Encoding", "gzip")
            .header("Cache-Control", "no-cache")
            .body(observabilityService.getSystemMetrics(searchFilter))
        ;
    }

    ///..
    @GetMapping(path = "/sessions-metadata", produces = "application/json")
    public @NonNull ResponseEntity<List<SessionMetadata>> getSessionsMetadata() {

        return ResponseEntity.ok().header("Cache-Control", "no-cache").body(sessionService.getSessionsMetadata());
    }

    ///..
    @PostMapping(path = "/logs", consumes = "application/json", produces = "application/json")
    public @NonNull ResponseEntity<StreamingResponseBody> getLogs(@RequestBody(required = true) @NonNull final LogSearchFilter logSearchFilter)
    throws MongoException {

        return ResponseEntity.ok().header("Content-Encoding", "gzip").body(logsService.getLogs(logSearchFilter));
    }

    ///..
    @GetMapping(path = "/fallback-logs", produces = "application/json")
    public @NonNull ResponseEntity<StreamingResponseBody> getFallbackLogs() {

        return ResponseEntity

            .ok()
            .header("Content-Encoding", "gzip")
            .header("Cache-Control", "no-cache")
            .body(logsService.getFallbackLogs())
        ;
    }

    ///
}
