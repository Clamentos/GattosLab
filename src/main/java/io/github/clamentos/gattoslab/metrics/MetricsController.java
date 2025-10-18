package io.github.clamentos.gattoslab.metrics;

///
import java.util.Map;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

///
@RestController
@RequestMapping("/gattos-lab")

///
public class MetricsController {

    ///
    private final MetricsService metricsService;

    ///
    @Autowired
    public MetricsController(final MetricsService metricsService) {

        this.metricsService = metricsService;
    }

    ///
    @GetMapping("/metrics/total-requests")
    public ResponseEntity<Map<String, Long>> getMetrics() {

        return ResponseEntity.ok(metricsService.getTotalRequests());
    }

    ///
}
