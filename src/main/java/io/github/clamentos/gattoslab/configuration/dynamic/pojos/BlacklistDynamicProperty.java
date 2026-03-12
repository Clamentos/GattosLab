package io.github.clamentos.gattoslab.configuration.dynamic.pojos;

///
import java.util.List;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class BlacklistDynamicProperty {

    ///
    private final List<BlacklistIpEntry> ipv4s;
    private final List<BlacklistIpEntry> ipv6s;
    private final List<String> userAgentContains;

    ///
}
