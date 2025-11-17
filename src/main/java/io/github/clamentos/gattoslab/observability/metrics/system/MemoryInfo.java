package io.github.clamentos.gattoslab.observability.metrics.system;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class MemoryInfo {

    ///
    private final MemoryInfoEntry heapUsage;
    private final MemoryInfoEntry nonHeapUsage;

    ///
}
