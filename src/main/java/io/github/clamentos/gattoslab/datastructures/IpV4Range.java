package io.github.clamentos.gattoslab.datastructures;

///
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

///
@AllArgsConstructor
@EqualsAndHashCode
@Getter

///
public final class IpV4Range {

    ///
    private final long start;
    private final long end;

    ///
}
