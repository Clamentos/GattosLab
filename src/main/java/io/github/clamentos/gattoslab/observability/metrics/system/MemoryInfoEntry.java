package io.github.clamentos.gattoslab.observability.metrics.system;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class MemoryInfoEntry {

    ///
    private final long initial;
    private final long inUse;
    private final long allocated;
    private final long maximum;

    ///
}
