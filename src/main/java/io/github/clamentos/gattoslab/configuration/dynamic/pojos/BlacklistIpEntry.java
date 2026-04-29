package io.github.clamentos.gattoslab.configuration.dynamic.pojos;

///
import io.github.clamentos.gattoslab.utils.Hashable;

///..
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

///
@AllArgsConstructor
@EqualsAndHashCode
@Getter

///
public final class BlacklistIpEntry implements Hashable {

    ///
    private final byte[] start;
    private final byte[] end;

    ///
}
