package io.github.clamentos.gattoslab.ingress.filters;

///
import io.github.clamentos.gattoslab.configuration.dynamic.DynamicProperties;
import io.github.clamentos.gattoslab.configuration.dynamic.DynamicPropertyEntity;
import io.github.clamentos.gattoslab.configuration.dynamic.DynamicPropertyType;
import io.github.clamentos.gattoslab.configuration.dynamic.pojos.BlacklistDynamicProperty;
import io.github.clamentos.gattoslab.configuration.dynamic.pojos.BlacklistIpEntry;
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.http.HttpUtils;
import io.github.clamentos.gattoslab.observability.logging.SquashedLogsContainer;
import io.github.clamentos.gattoslab.observability.logging.squash.SquashLogEventType;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

///..
import java.net.InetAddress;
import java.util.List;
import java.util.Set;

///..
import lombok.AllArgsConstructor;

///
@AllArgsConstructor

///
public final class BlacklistFilter {

    ///
    private final DynamicProperties dynamicProperties;
    private final SquashedLogsContainer squashedLogContainer;

    ///
    public void isAllowed(final HttpServerExchange exchange) throws ApiSecurityException {

        final DynamicPropertyEntity<BlacklistDynamicProperty> blacklist = dynamicProperties.get(DynamicPropertyType.BLACKLIST);
        if(blacklist == null) return;

        final BlacklistDynamicProperty property = blacklist.getValue();
        final InetAddress ip = exchange.getSourceAddress().getAddress();
        final String userAgent = HttpUtils.getHeaderValue(exchange.getRequestHeaders(), Headers.USER_AGENT_STRING);

        this.isIpAllowed(ip, property.getIpv4s(), userAgent);
        this.isIpAllowed(ip, property.getIpv6s(), userAgent);

        final Set<String> userAgentContains = property.getUserAgentContains();
        if(userAgentContains.isEmpty() || userAgent == null) return;

        for(final String piece : userAgentContains) {

            if(userAgent.contains(piece)) throw this.createException(ip, userAgent); 
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

            return !(this.compareIpv6(address, start) == -1 || this.compareIpv6(address, end) == 1);
        }
    }

    ///..
    private long ipv4ToLong(final byte[] octets) {

        long ipv4Long = 0;

        ipv4Long |= ((long)octets[0] & 0x0FF) << 24;
        ipv4Long |= ((long)octets[1] & 0x0FF) << 16;
        ipv4Long |= ((long)octets[2] & 0x0FF) << 8;
        ipv4Long |= ((long)octets[3] & 0x0FF);

        return ipv4Long;
    }

    ///..
    private int compareIpv6(final byte[] octetsA, final byte[] octetsB) {

        for(int i = 0; i < octetsA.length; i++) {

            final int a = (octetsA[i] & 0x0FF);
            final int b = (octetsB[i] & 0x0FF);

            if(a < b) return -1;
            else if(a > b) return 1;
        }

        return 0;
    }

    ///..
    private ApiSecurityException createException(final InetAddress ip, final String userAgent) {

        squashedLogContainer.squash(SquashLogEventType.BLACKLISTED, GenericUtils.composeFingerprint(ip, userAgent));
        return new ApiSecurityException("BlacklistFilter.createException~Blacklisted");
    }

    ///
}
