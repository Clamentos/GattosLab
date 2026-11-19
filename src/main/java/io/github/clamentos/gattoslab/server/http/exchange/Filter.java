package io.github.clamentos.gattoslab.server.http.exchange;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.components.HttpProxy;
import io.github.clamentos.gattoslab.datastructures.Pair;
import io.github.clamentos.gattoslab.datastructures.addresses.IpAddress;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.security.SessionService;
import io.github.clamentos.gattoslab.security.components.AuthorizationAction;
import io.github.clamentos.gattoslab.security.components.SecurityFailure;
import io.github.clamentos.gattoslab.server.http.exchange.components.HttpExchange;
import io.github.clamentos.gattoslab.server.http.exchange.components.HttpHeader;
import io.github.clamentos.gattoslab.server.http.exchange.components.HttpMethod;
import io.github.clamentos.gattoslab.server.http.exchange.components.HttpStatus;
import io.github.clamentos.gattoslab.server.http.exchange.components.MimeType;
import io.github.clamentos.gattoslab.server.http.exchange.components.RateLimitEntry;
import io.github.clamentos.gattoslab.server.http.exchange.components.resources.Resource;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

///
public final class Filter extends Responder {

    ///
    private static final String RATE_LIMITED_MESSAGE = "Rate limited";
    private static final String ILLEGAL_HOST_MESSAGE = "Unknown or blank Host header";

    private static final byte[] RATE_LIMITED_MESSAGE_BYTES = RATE_LIMITED_MESSAGE.getBytes();
    private static final byte[] ILLEGAL_HOST_MESSAGE_BYTES = ILLEGAL_HOST_MESSAGE.getBytes();
    private static final byte[] NOT_FOUND_MESSAGE_BYTES = "Not found".getBytes();

    ///.
    private final SquashingLogger squashingLogger;

    ///..
    private final ResourceMappings resourceMappings;
    private final SessionService sessionService;

    ///..
    private final String[] illegalPathContains;
    private final Map<IpAddress, RateLimitEntry> rateLimitTracker;

    ///..
    private final Map<HttpHeader, String> siteCorsHeaders;
    private final Map<HttpHeader, String> dashboardCorsHeaders;

    ///..
    private final int rateLimitAmount;
    private final int entryCounterStart;

    ///
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

        this.illegalPathContains = applicationProperties.getIllegalPathContains().stream().toArray(String[]::new);
        this.rateLimitTracker = new ConcurrentHashMap<>();

        this.siteCorsHeaders = new EnumMap<>(HttpHeader.class);
        this.siteCorsHeaders.putAll(ApplicationProperties.CORS_HEADERS);

        this.siteCorsHeaders.put(

            HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN,
            GenericUtils.concatenateAsCsv(applicationProperties.getSiteAllowedOrigins())
        );

        this.dashboardCorsHeaders = new EnumMap<>(HttpHeader.class);
        this.dashboardCorsHeaders.putAll(ApplicationProperties.CORS_HEADERS);

        this.dashboardCorsHeaders.put(

            HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN,
            GenericUtils.concatenateAsCsv(applicationProperties.getDashboardAllowedOrigins())
        );

        this.rateLimitAmount = applicationProperties.getRateLimitAmount();

        this.entryCounterStart = (int)(ApplicationProperties.RATE_LIMIT_BLOCK_DURATION.toMillis() / batchScheduler.schedule(

            this::replenishTask,
            "gattos-lab-rate-limit-replenish-task",
            ApplicationProperties.RATE_LIMIT_REPLENISH_CRON
        ));
    }

    ///
    public boolean filter(final HttpExchange exchange, final boolean isDashboard) {

        super.observabilityService.requestStarted();

        if(!this.doRateLimitFilter(exchange)) return false;
        if(!this.doIngressFilter(exchange)) return false;

        if(isDashboard) return this.authForDashboard(exchange);
        return this.authForPublic(exchange);
    }

    ///.
    private boolean doRateLimitFilter(final HttpExchange exchange) {

        final RateLimitEntry rateLimitEntry = this.rateLimitTracker.computeIfAbsent(

            exchange.getRemoteAddress(),
            _ -> new RateLimitEntry(this.rateLimitAmount, this.entryCounterStart)
        );

        if(rateLimitEntry.isRateLimited()) {

            this.squashingLogger.warning(GenericUtils.composeMessageForSquash(RATE_LIMITED_MESSAGE, exchange));
            exchange.requestClose();

            super.respond(

                exchange,
                HttpStatus.TOO_MANY_REQUESTS,
                ApplicationProperties.RETRY_AFTER_HEADERS,
                MimeType.TEXT,
                RATE_LIMITED_MESSAGE_BYTES
            );

            return false;
        }

        return true;
    }

    ///..
    private boolean doIngressFilter(final HttpExchange exchange) {

        final String uri = exchange.getUri();

        for(final String illegalElement : this.illegalPathContains) {

            if(uri.contains(illegalElement)) {

                exchange.setUriBad(true);
                break;
            }
        }

        if(exchange.getMethod() == HttpMethod.OPTIONS) {

            super.respond(exchange, HttpStatus.NO_CONTENT, this.siteCorsHeaders);
            return false;
        }

        return true;
    }

    ///..
    private boolean authForDashboard(final HttpExchange exchange) {

        final Resource resource = this.resourceMappings.getDashboard(exchange.getUri());
        final String domain = exchange.getRequestHeaders().get(HttpHeader.HOST);

        exchange.setDomain(domain);

        if(!this.checkFound(exchange, resource)) return false;
        if(resource.getAuthorizationAction() != AuthorizationAction.ALLOW) return this.doAuth(exchange);

        return true;
    }

    ///..
    private boolean authForPublic(final HttpExchange exchange) {

        final String domain = exchange.getRequestHeaders().get(HttpHeader.HOST);
        final Pair<HttpProxy, Void> domainInformation = this.resourceMappings.getDomainInformation(domain);

        if(domainInformation == null) {

            this.squashingLogger.warning(GenericUtils.composeMessageForSquash(ILLEGAL_HOST_MESSAGE, exchange));

            exchange.requestClose();
            super.respond(exchange, HttpStatus.BAD_REQUEST, MimeType.TEXT, ILLEGAL_HOST_MESSAGE_BYTES);

            return false;
        }

        final String uri = exchange.getUri();
        final HttpProxy proxy = domainInformation.getA();

        exchange.setProxy(proxy);
        exchange.setDomain(domain);

        if(proxy != null) {

            if(proxy.isAuthenticated() && this.resourceMappings.requiresAuth(uri)) {

                return this.doAuth(exchange);
            }

            return true;
        }

        return this.checkFound(exchange, this.resourceMappings.getSite(domain, uri));
    }

    ///..
    private boolean doAuth(final HttpExchange exchange) {

        final SecurityFailure securityFailure = this.sessionService.isAllowed(exchange);

        if(securityFailure != null) {

            super.respond(exchange, HttpStatus.SEE_OTHER, ApplicationProperties.LOGIN_REDIRECT_HEADERS);
            return false;
        }

        return true;
    }

    ///..
    private boolean checkFound(final HttpExchange exchange, final Resource resource) {

        if(resource == null) {

            super.respond(exchange, HttpStatus.NOT_FOUND, MimeType.TEXT, NOT_FOUND_MESSAGE_BYTES);
            return false;
        }

        exchange.setResource(resource);
        return true;
    }

    ///..
    private void replenishTask() {

        for(final Entry<IpAddress, RateLimitEntry> entry : this.rateLimitTracker.entrySet()) {

            if(entry.getValue().replenish(this.rateLimitAmount)) this.rateLimitTracker.remove(entry.getKey());
        }
    }

    ///
}
