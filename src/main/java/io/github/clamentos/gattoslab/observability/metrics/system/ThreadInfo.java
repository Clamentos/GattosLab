package io.github.clamentos.gattoslab.observability.metrics.system;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class ThreadInfo {

    ///
    private final int platformThreadCount;
    private final int daemonThreadCount;
    private final int peakThreadCount;
    private final double averageCpuLoad;

    ///
}
