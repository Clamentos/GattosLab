package io.github.clamentos.gattoslab.observability.metrics.system;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class SystemStatus {

    ///
    private final RuntimeInfo runtimeInfo;
    private final MemoryInfo memoryInfo;
    private final ThreadInfo threadInfo;

    ///
}
