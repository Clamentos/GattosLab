package io.github.clamentos.gattoslab.configuration.dynamic.entities;

///
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

///..
import io.github.clamentos.gattoslab.exceptions.ValidationException;

///..
import java.util.List;
import java.util.Set;

///..
import lombok.EqualsAndHashCode;
import lombok.Getter;

///
@EqualsAndHashCode(callSuper = true)
@Getter

///
public final class BlacklistDynamicProperty extends DynamicPropertyEntity {

    ///
    private final List<BlacklistIpEntry> ipv4s;
    private final List<BlacklistIpEntry> ipv6s;
    private final Set<String> userAgentContains;

    ///
    @JsonCreator
    public BlacklistDynamicProperty(

        @JsonProperty("type") final DynamicPropertyType type,
        @JsonProperty("enabled") final boolean enabled,
        @JsonProperty("ipv4s") final List<BlacklistIpEntry> ipv4s,
        @JsonProperty("ipv6s") final List<BlacklistIpEntry> ipv6s,
        @JsonProperty("userAgentContains") final Set<String> userAgentContains

    ) throws ValidationException {

        super(type, enabled);

        this.ipv4s = ipv4s != null ? ipv4s : List.of();
        this.ipv6s = ipv6s != null ? ipv6s : List.of();

        if(userAgentContains != null) {

            for(final String pattern : userAgentContains) {

                if(pattern == null || pattern.isBlank()) {

                    throw new ValidationException("Field 'userAgentContains' cannot contain null or blank elements", "BlacklistDynamicProperty.<init>");
                }
            }

            this.userAgentContains = userAgentContains;
        }

        else this.userAgentContains = Set.of();
    }

    ///
}
