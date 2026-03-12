package io.github.clamentos.gattoslab.configuration.dynamic.pojos;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class BlacklistIpEntry {

    ///
    private final byte[] start;
    private final byte[] end;

    ///
}
