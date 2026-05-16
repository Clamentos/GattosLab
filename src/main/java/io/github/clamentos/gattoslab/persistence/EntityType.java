package io.github.clamentos.gattoslab.persistence;

///
import java.nio.file.Path;

///..
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter

///
public enum EntityType {

    ///
    LOGS(Path.of("./observability/logs/"), 15, 28),                                               // gattoslab-logs-yyyy-mm-dd-HH-idx.log
    REQUEST_METRICS(Path.of("./observability/request_metrics/"), 26, 39),                         // gattoslab-request-metrics-yyyy-mm-dd-HH-idx.log
    SYSTEM_METRICS(Path.of("./observability/system_metrics/"), 25, 38),                           // gattoslab-system-metrics-yyyy-mm-dd-HH-idx.log
    DYNAMIC_PROPERTIES(Path.of("./observability/dynamic_properties/gattoslab.conf"), -1, -1);     // gattoslab.conf

    ///
    private final Path path;
    private final int dateStartIndex;
    private final int dateEndIndex;

    ///
}
