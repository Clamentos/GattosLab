package io.github.clamentos.gattoslab.exchange.filters;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.HashCodedByteArray;
import io.github.clamentos.gattoslab.datastructures.IpV4Range;
import io.github.clamentos.gattoslab.datastructures.Pair;
import io.github.clamentos.gattoslab.exchange.Responder;
import io.github.clamentos.gattoslab.exchange.filters.components.AuthorizationAction;
import io.github.clamentos.gattoslab.exchange.filters.components.RateLimitEntry;
import io.github.clamentos.gattoslab.exchange.handling.ResourceMappings;
import io.github.clamentos.gattoslab.exchange.handling.components.Resource;
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.http.server.HttpExchange;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.security.SecurityFailure;
import io.github.clamentos.gattoslab.security.SessionService;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

///
public final class Filter extends Responder {

    ///
    private static final String BLOCKED_MESSAGE = "Blocked";
    private static final byte[] BLOCKED_MESSAGE_BYTES = BLOCKED_MESSAGE.getBytes();
    private static final String RATE_LIMITED_MESSAGE = "Rate limited";
    private static final byte[] RATE_LIMITED_MESSAGE_BYTES = RATE_LIMITED_MESSAGE.getBytes();

    ///.
    private final SquashingLogger squashingLogger;

    ///..
    private final ResourceMappings resourceMappings;
    private final SessionService sessionService;

    ///..
    private final IpV4Range[] blockedIpV4s;
    private final Pair<byte[], byte[]>[] blockedIpV6s;
    private final String[] illegalUserAgentContains;
    private final Map<HashCodedByteArray, RateLimitEntry> rateLimitMap;

    ///..
    private final Map<HttpHeader, String> corsHeaders;

    ///..
    private final int rateLimitAmount;
    private final int entryCounterStart;

    ///
    @SuppressWarnings("unchecked")
    public Filter(

        final ObservabilityService observabilityService,
        final ApplicationProperties applicationProperties,
        final SquashingLogger squashingLogger,
        final ResourceMappings resourceMappings,
        final SessionService sessionService,
        final BatchScheduler batchScheduler
    ) {

        super(observabilityService);

        this.squashingLogger = squashingLogger;

        this.resourceMappings = resourceMappings;
        this.sessionService = sessionService;

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
        this.rateLimitMap = new ConcurrentHashMap<>();

        this.corsHeaders = new EnumMap<>(HttpHeader.class);
        this.corsHeaders.putAll(ApplicationProperties.CORS_HEADERS);
        this.corsHeaders.put(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, GenericUtils.concatenateAsCsv(applicationProperties.getAllowedOrigins()));

        this.rateLimitAmount = applicationProperties.getRateLimitAmount();

        this.entryCounterStart = (int)(ApplicationProperties.RATE_LIMIT_BLOCK_DURATION.toMillis() / batchScheduler.schedule(

            this::replenishTask,
            "gattos-lab-rate-limit-replenish-task",
            ApplicationProperties.RATE_LIMIT_REPLENISH_CRON
        ));
    }

    ///
    public boolean filter(final HttpExchange exchange) {

        if(!this.doIngressFilter(exchange)) return false;
        if(!this.doRateLimitFilter(exchange)) return false;

        return this.doAuthorizationFilter(exchange);
    }

    ///.
    private boolean doIngressFilter(final HttpExchange exchange) {

        super.observabilityService.requestStarted();

        final String userAgent = exchange.getRequestHeaders().get(HttpHeader.USER_AGENT);
        exchange.setResource(this.resourceMappings.get(exchange.getUri()));

        if(this.isBlocked(exchange.getRemoteIpAddress())) {

            this.respondBlocked(exchange);
            return false;
        }

        for(final String illegalContains : this.illegalUserAgentContains) {

            if(
                (illegalContains == null && userAgent == null) ||
                ("".equals(illegalContains) && "".equals(userAgent)) ||
                (illegalContains != null && userAgent != null && illegalContains.toLowerCase(Locale.US).contains(userAgent.toLowerCase(Locale.US)))
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

    ///..
    private boolean doRateLimitFilter(final HttpExchange exchange) {

        final RateLimitEntry rateLimitEntry = this.rateLimitMap.computeIfAbsent(

            new HashCodedByteArray(exchange.getRemoteIpAddress()),
            _ -> new RateLimitEntry(this.rateLimitAmount, this.entryCounterStart)
        );

        if(rateLimitEntry.isRateLimited()) {

            this.squashingLogger.warning(GenericUtils.composeMessageForSquash(RATE_LIMITED_MESSAGE, exchange));
            super.respond(exchange, HttpStatus.TOO_MANY_REQUESTS, ApplicationProperties.RETRY_AFTER_HEADERS, MimeType.TEXT, RATE_LIMITED_MESSAGE_BYTES);
            exchange.close();

            return false;
        }

        return true;
    }

    ///..
    private boolean doAuthorizationFilter(final HttpExchange exchange) {

        final Resource resource = exchange.getResource();
        if(resource == null) return true;

        final AuthorizationAction authorizationAction = resource.getAuthorizationAction();

        if(authorizationAction != AuthorizationAction.ALLOW) {

            final SecurityFailure securityFailure = this.sessionService.isAllowed(exchange);

            if(securityFailure != null) {

                if(authorizationAction == AuthorizationAction.REDIRECT) {

                    super.respond(exchange, HttpStatus.SEE_OTHER, ApplicationProperties.LOGIN_REDIRECT_HEADERS);
                }

                else {

                    this.squashingLogger.warning(GenericUtils.composeMessageForSquash("Authorization failed for", exchange));
                    super.respond(exchange, HttpStatus.UNAUTHORIZED, MimeType.TEXT, securityFailure.getMessage());
                    exchange.close();
                }

                return false;
            }
        }

        return true;
    }

    ///..
    private boolean isBlocked(final byte[] address) {

        final int addressLength = address.length;

        if(this.blockedIpV4s.length > 0 && addressLength == 4) {

            final long addressAsLong = GenericUtils.ipV4ToLong(address);

            for(final IpV4Range range : this.blockedIpV4s) {

                if(addressAsLong >= range.getStart() && addressAsLong <= range.getEnd()) return true;
            }

            return false;
        }

        if(this.blockedIpV6s.length > 0 && addressLength == 16) {

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

    ///..
    private void replenishTask() {

        for(final Entry<HashCodedByteArray, RateLimitEntry> entry : this.rateLimitMap.entrySet()) {

            if(entry.getValue().replenish(this.rateLimitAmount)) this.rateLimitMap.remove(entry.getKey());
        }
    }

    ///
}
