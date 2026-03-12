package io.github.clamentos.gattoslab.ingress.filters;

///
import io.github.clamentos.gattoslab.configuration.dynamic.DynamicProperties;
import io.github.clamentos.gattoslab.configuration.dynamic.DynamicPropertyEntity;
import io.github.clamentos.gattoslab.configuration.dynamic.DynamicPropertyType;
import io.github.clamentos.gattoslab.configuration.dynamic.pojos.BlacklistDynamicProperty;
import io.github.clamentos.gattoslab.configuration.dynamic.pojos.BlacklistIpEntry;
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.observability.logging.SquashedLogContainer;
import io.github.clamentos.gattoslab.observability.logging.squash.SquashLogEventType;
import io.github.clamentos.gattoslab.utils.GenericUtils;
import io.github.clamentos.gattoslab.utils.HttpUtils;

///..
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

///..
import java.net.InetAddress;
import java.util.List;

///..
import lombok.AllArgsConstructor;

///
@AllArgsConstructor

///
public final class BlacklistFilter {

    ///
    private final DynamicProperties dynamicProperties;
    private final SquashedLogContainer squashedLogContainer;

    ///
    public void isAllowed(final HttpServerExchange exchange) throws ApiSecurityException {

        final DynamicPropertyEntity<BlacklistDynamicProperty> blacklist = dynamicProperties.get(DynamicPropertyType.BLACKLIST);
        if(blacklist == null) return;

        final BlacklistDynamicProperty value = blacklist.getValue();
        final InetAddress ip = exchange.getSourceAddress().getAddress();
        final String userAgent = HttpUtils.getHeaderValue(exchange.getRequestHeaders(), Headers.USER_AGENT_STRING);

        this.isIpAllowed(ip, value.getIpv4s(), userAgent);
        this.isIpAllowed(ip, value.getIpv6s(), userAgent);

        final List<String> userAgentContains = value.getUserAgentContains();
        if(userAgentContains.isEmpty() || userAgent == null) return;

        for(final String contains : userAgentContains) {

            if(userAgent.contains(contains)) throw this.createException(ip, userAgent); 
        }
    }

    ///.
    private void isIpAllowed(final InetAddress ip, final List<BlacklistIpEntry> ranges, final String userAgent) throws ApiSecurityException {

        for(final BlacklistIpEntry range : ranges) {

            if(this.isInRange(ip, range.getStart(), range.getEnd())) throw this.createException(ip, userAgent);
        }
    }

    ///..
    private boolean isInRange(final InetAddress ip, final byte[] start, final byte[] end) {

        final byte[] address = ip.getAddress();

        if(address.length == 4) {

            final long toTest = this.ipv4ToLong(address);
            return toTest >= this.ipv4ToLong(start) && toTest <= this.ipv4ToLong(end);
        }

        else {

            for(int i = 0; i < address.length; i++) {

                final byte elem = address[i];
                if(elem < start[i] || elem > end[i]) return false;
            }

            return true;
        }
    }

    ///..
    private long ipv4ToLong(final byte[] octets) {

        long ipv4Long = 0;

        ipv4Long |= octets[0] << 24;
        ipv4Long |= octets[1] << 16;
        ipv4Long |= octets[2] << 8;
        ipv4Long |= octets[3];

        return ipv4Long;
    }

    ///..
    private ApiSecurityException createException(final InetAddress ip, final String userAgent) {

        squashedLogContainer.squash(SquashLogEventType.BLACKLISTED, GenericUtils.composeFingerprint(ip, userAgent));
        return new ApiSecurityException("Blacklisted");
    }

    ///
}
