package io.github.clamentos.gattoslab.configuration.dynamic.pojos;

///
import io.github.clamentos.gattoslab.utils.Hashable;

///..
import java.util.List;
import java.util.Set;

///..
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

///
@AllArgsConstructor
@EqualsAndHashCode
@Getter

///
public final class BlacklistDynamicProperty implements Hashable {

    ///
    private final List<BlacklistIpEntry> ipv4s;
    private final List<BlacklistIpEntry> ipv6s;
    private final Set<String> userAgentContains;

    ///
}
