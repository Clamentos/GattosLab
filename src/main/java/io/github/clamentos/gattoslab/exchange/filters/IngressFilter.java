package io.github.clamentos.gattoslab.exchange.filters;

///
import com.sun.net.httpserver.HttpExchange;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.IpV4Range;
import io.github.clamentos.gattoslab.exchange.Responder;
import io.github.clamentos.gattoslab.exchange.handling.ResourceMappings;
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicLong;

///
public final class IngressFilter extends Responder {

    ///
    private final SquashingLogger squashingLogger;
    private final ResourceMappings resourceMappings;

    ///..
    private final AtomicLong requestCounter;

    ///..
    private final IpV4Range[] blockedIpV4s;
    private final Entry<byte[], byte[]>[] blockedIpV6s;
    private final String[] illegalUserAgentContains;

    ///..
    private final Map<String, List<String>> corsHeaders;

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

        this.requestCounter = new AtomicLong();

        this.blockedIpV4s = applicationProperties.getBlockedIpV4s()

            .stream()
            .map(e -> new IpV4Range(

                GenericUtils.ipV4ToLong(e.getKey().getAddress()),
                GenericUtils.ipV4ToLong(e.getValue().getAddress())
            ))
            .toArray(IpV4Range[]::new)
        ;

        this.blockedIpV6s = applicationProperties.getBlockedIpV6s()

            .stream()
            .map(e -> Map.entry(e.getKey().getAddress(), e.getValue().getAddress()))
            .toArray(Entry[]::new)
        ;

        this.illegalUserAgentContains = applicationProperties.getIllegalUserAgentContains().stream().toArray(String[]::new);

        this.corsHeaders = ApplicationProperties.CORS_HEADERS;
        this.corsHeaders.put(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN.getName(), applicationProperties.getAllowedOrigins().stream().toList());
    }

    ///
    public boolean isOk(final HttpExchange exchange) {

        super.observabilityService.requestStarted();
        exchange.setAttribute(ApplicationProperties.REQUEST_START_TIME_ATTRIBUTE, System.currentTimeMillis());

        final String userAgent = exchange.getRequestHeaders().getFirst(HttpHeader.USER_AGENT.getName());
        final byte[] rawAddress = exchange.getRemoteAddress().getAddress().getAddress();

        exchange.setAttribute(ApplicationProperties.REQUEST_REQUEST_ID_ATTRIBUTE, this.requestCounter.getAndIncrement());
        exchange.setAttribute(ApplicationProperties.REQUEST_USER_AGENT_ATTRIBUTE, userAgent);
        exchange.setAttribute(ApplicationProperties.REQUEST_RESOURCE_ATTRIBUTE, this.resourceMappings.get(exchange.getRequestURI().getPath()));
        exchange.setAttribute(ApplicationProperties.REQUEST_RAW_ADDRESS_ATTRIBUTE, rawAddress);

        if(this.isBlocked(rawAddress)) {

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

        final HttpMethod method = HttpMethod.decode(exchange.getRequestMethod());

        if(method == HttpMethod.OPTIONS) {

            super.respond(exchange, HttpStatus.NO_CONTENT, this.corsHeaders);
            return false;
        }

        exchange.setAttribute(ApplicationProperties.REQUEST_METHOD_ATTRIBUTE, method);
        return true;
    }

    ///..
    private boolean isBlocked(final byte[] address) {

        if(this.blockedIpV4s.length > 0 && address.length == 4) {

            final long addressAsLong = GenericUtils.ipV4ToLong(address);

            for(final IpV4Range range : this.blockedIpV4s) {

                if(addressAsLong >= range.getStart() && addressAsLong <= range.getEnd()) return true;
            }

            return false;
        }

        if(this.blockedIpV6s.length > 0) {

            for(final Entry<byte[], byte[]> range : this.blockedIpV6s) {

                if(Arrays.compare(address, range.getKey()) >= 0 && Arrays.compare(address, range.getValue()) <= 0) return true;
            }

            return false;
        }

        return false;
    }

    ///..
    private void respondBlocked(final HttpExchange exchange) {

        this.squashingLogger.warning("Blocked '" + GenericUtils.composeFingerprint(exchange) + "' # times");

        super.respond(exchange, HttpStatus.FORBIDDEN);
        exchange.close();
    }

    ///
}
