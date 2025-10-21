package io.github.clamentos.gattoslab.metrics;

///
import java.util.Map;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

///
@RestController
@RequestMapping("/gattos-lab")

///
public class MetricsController {

    ///
    private final MetricsService metricsService;
    private final String apiKey;

    ///
    @Autowired
    public MetricsController(final MetricsService metricsService, @Value("${apiKey}") final String apiKey) {

        this.metricsService = metricsService;
        this.apiKey = apiKey;
    }

    ///
    @GetMapping(value = "/metrics/total-requests", produces = "application/json")
    public ResponseEntity<Map<String, Object>> getMetrics(@RequestParam(value = "api_key", required = false) final String apiKey) {

        if(apiKey == null || !apiKey.equals(this.apiKey)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(metricsService.getHitsTrackerData());
    }

    ///
}
