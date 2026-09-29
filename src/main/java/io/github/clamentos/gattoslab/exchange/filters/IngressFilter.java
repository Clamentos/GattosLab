package io.github.clamentos.gattoslab.exchange.filters;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.IpV4Range;
import io.github.clamentos.gattoslab.datastructures.Pair;
import io.github.clamentos.gattoslab.exchange.Responder;
import io.github.clamentos.gattoslab.exchange.handling.ResourceMappings;
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.http.server.Filter;
import io.github.clamentos.gattoslab.http.server.HttpExchange;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

///
public final class IngressFilter extends Responder implements Filter {

    ///
    private static final String BLOCKED_MESSAGE = "Blocked";
    private static final byte[] BLOCKED_MESSAGE_BYTES = BLOCKED_MESSAGE.getBytes();

    ///.
    private final SquashingLogger squashingLogger;
    private final ResourceMappings resourceMappings;

    ///..
    private final IpV4Range[] blockedIpV4s;
    private final Pair<byte[], byte[]>[] blockedIpV6s;
    private final String[] illegalUserAgentContains;

    ///..
    private final Map<HttpHeader, String> corsHeaders;

    ///
    @SuppressWarnings("unchecked")
    public IngressFilter(

        final ObservabilityService observabilityService,
        final ApplicationProperties applicationProperties,
        final SquashingLogger squashingLogger,
        final ResourceMappings resourceMappings
    ) {

        super(observabilityService);

        this.squashingLogger = squashingLogger;
        this.resourceMappings = resourceMappings;

        this.blockedIpV4s = applicationProperties.getBlockedIpV4s()

            .stream()
            .map(e -> new IpV4Range(

                GenericUtils.ipV4ToLong(e.getA().getAddress()),
                GenericUtils.ipV4ToLong(e.getB().getAddress())
            ))
            .toArray(IpV4Range[]::new)
        ;

        this.blockedIpV6s = applicationProperties.getBlockedIpV6s()

            .stream()
            .map(e -> new Pair<>(e.getA().getAddress(), e.getB().getAddress()))
            .toArray(Pair[]::new)
        ;

        this.illegalUserAgentContains = applicationProperties.getIllegalUserAgentContains().stream().toArray(String[]::new);

        this.corsHeaders = new EnumMap<>(HttpHeader.class);
        this.corsHeaders.putAll(ApplicationProperties.CORS_HEADERS);

        this.corsHeaders.put(

            HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN,
            GenericUtils.concatenateAsCsv(applicationProperties.getAllowedOrigins())
        );
    }

    ///
    @Override
    public boolean filter(final HttpExchange exchange) {

        super.observabilityService.requestStarted();

        final String userAgent = exchange.getRequestHeaders().get(HttpHeader.USER_AGENT);
        exchange.setResource(this.resourceMappings.get(exchange.getPath()));

        if(this.isBlocked(exchange.getRemoteAddress())) {

            this.respondBlocked(exchange);
            return false;
        }

        for(final String illegalUserAgentContain : this.illegalUserAgentContains) {

            if(
                (illegalUserAgentContain == null && userAgent == null) ||
                ("".equals(illegalUserAgentContain) && "".equals(userAgent)) ||
                (illegalUserAgentContain != null && illegalUserAgentContain.equalsIgnoreCase(userAgent))
            ) {
    
                this.respondBlocked(exchange);
                return false;
            }
        }

        if(exchange.getMethod() == HttpMethod.OPTIONS) {

            super.respond(exchange, HttpStatus.NO_CONTENT, this.corsHeaders);
            return false;
        }

        return true;
    }

    ///.
    private boolean isBlocked(final byte[] address) {

        if(this.blockedIpV4s.length > 0 && address.length == 4) {

            final long addressAsLong = GenericUtils.ipV4ToLong(address);

            for(final IpV4Range range : this.blockedIpV4s) {

                if(addressAsLong >= range.getStart() && addressAsLong <= range.getEnd()) return true;
            }

            return false;
        }

        if(this.blockedIpV6s.length > 0) {

            for(final Pair<byte[], byte[]> range : this.blockedIpV6s) {

                if(Arrays.compare(address, range.getA()) >= 0 && Arrays.compare(address, range.getB()) <= 0) return true;
            }

            return false;
        }

        return false;
    }

    ///..
    private void respondBlocked(final HttpExchange exchange) {

        this.squashingLogger.warning(GenericUtils.composeMessageForSquash(BLOCKED_MESSAGE, exchange));
        super.respond(exchange, HttpStatus.FORBIDDEN, MimeType.TEXT, BLOCKED_MESSAGE_BYTES);
        exchange.close();
    }

    ///
}
