package io.github.clamentos.gattoslab.ingress.filters;

///
import io.github.clamentos.gattoslab.configuration.dynamic.DynamicProperties;
import io.github.clamentos.gattoslab.configuration.dynamic.entities.BlacklistDynamicProperty;
import io.github.clamentos.gattoslab.configuration.dynamic.entities.BlacklistIpEntry;
import io.github.clamentos.gattoslab.configuration.dynamic.entities.DynamicPropertyType;
import io.github.clamentos.gattoslab.exceptions.BlacklistedException;
import io.github.clamentos.gattoslab.exceptions.handling.GlobalExceptionHandler;
import io.github.clamentos.gattoslab.http.HttpUtils;

///..
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.spec.HttpServletRequestImpl;
import io.undertow.util.Headers;

///..
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

///..
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

///..
import lombok.AllArgsConstructor;

///
@AllArgsConstructor

///
public final class BlacklistFilter implements Filter {

    ///
    private final DynamicProperties dynamicProperties;
    private final GlobalExceptionHandler globalExceptionHandler;

    ///
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {

        final HttpServerExchange exchange = ((HttpServletRequestImpl)request).getExchange();

        try {

            this.isAllowed(exchange);
            chain.doFilter(request, response);
        }

        catch(final BlacklistedException exc) {

            globalExceptionHandler.handle(exc, exchange);
        }
    }

    ///.
    private void isAllowed(final HttpServerExchange exchange) throws BlacklistedException {

        final BlacklistDynamicProperty property = (BlacklistDynamicProperty) dynamicProperties.get(DynamicPropertyType.BLACKLIST);
        if(property == null) return;

        final InetAddress ip = exchange.getSourceAddress().getAddress();
        final String userAgent = HttpUtils.getHeaderValue(exchange.getRequestHeaders(), Headers.USER_AGENT_STRING);

        if(ip instanceof Inet4Address) this.isIpAllowed(ip, property.getIpv4s());
        else this.isIpAllowed(ip, property.getIpv6s());

        final Set<String> userAgentContains = property.getUserAgentContains();
        if(userAgentContains.isEmpty()) return;

        for(final String piece : userAgentContains) {

            if(userAgent.contains(piece)) throw this.createException();
        }
    }

    ///..
    private void isIpAllowed(final InetAddress ip, final List<BlacklistIpEntry> ranges)
    throws BlacklistedException {

        for(final BlacklistIpEntry range : ranges) {

            if(this.isInRange(ip, range.getStart(), range.getEnd())) throw this.createException();
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

            return !(Arrays.compareUnsigned(address, start) < 0 || Arrays.compareUnsigned(address, end) > 0);
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
    private BlacklistedException createException() {

        return new BlacklistedException("Blacklisted", "BlacklistFilter.createException");
    }

    ///
}
