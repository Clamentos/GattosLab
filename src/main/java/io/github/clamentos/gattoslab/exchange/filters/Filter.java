package io.github.clamentos.gattoslab.exchange.filters;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.address.IpAddress;
import io.github.clamentos.gattoslab.exchange.Responder;
import io.github.clamentos.gattoslab.exchange.filters.components.RateLimitEntry;
import io.github.clamentos.gattoslab.exchange.handling.ResourceMappings;
import io.github.clamentos.gattoslab.exchange.handling.components.Domain;
import io.github.clamentos.gattoslab.exchange.handling.components.Resource;
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.http.server.components.HttpExchange;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.security.SessionService;
import io.github.clamentos.gattoslab.security.components.AuthorizationAction;
import io.github.clamentos.gattoslab.security.components.SecurityFailure;
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
    private static final byte[] RATE_LIMITED_MESSAGE_BYTES = RATE_LIMITED_MESSAGE.getBytes();
    private static final String ILLEGAL_HOST_MESSAGE = "Unknown or blank Host header";
    private static final byte[] ILLEGAL_HOST_MESSAGE_BYTES = RATE_LIMITED_MESSAGE.getBytes();

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

        this.illegalPathContains = applicationProperties.getIllegalPathContains();
        this.rateLimitTracker = new ConcurrentHashMap<>();

        this.siteCorsHeaders = new EnumMap<>(HttpHeader.class);
        this.siteCorsHeaders.putAll(ApplicationProperties.CORS_HEADERS);
        this.siteCorsHeaders.put(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, GenericUtils.concatenateAsCsv(applicationProperties.getSiteAllowedOrigins()));

        this.dashboardCorsHeaders = new EnumMap<>(HttpHeader.class);
        this.dashboardCorsHeaders.putAll(ApplicationProperties.CORS_HEADERS);
        this.dashboardCorsHeaders.put(HttpHeader.ACCESS_CONTROL_ALLOW_ORIGIN, GenericUtils.concatenateAsCsv(applicationProperties.getDashboardAllowedOrigins()));

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
        if(!this.doIngressFilter(exchange, isDashboard)) return false;

        return this.doAuthorizationFilter(exchange, isDashboard);
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
    private boolean doIngressFilter(final HttpExchange exchange, final boolean isDashboard) {

        final String uri = exchange.getUri();

        if(!isDashboard) {

            final Domain host = Domain.valueOf(exchange.getRequestHeaders().get(HttpHeader.HOST));

            if(host == null) {

                exchange.requestClose();
                super.respond(exchange, HttpStatus.BAD_REQUEST, MimeType.TEXT, ILLEGAL_HOST_MESSAGE_BYTES);

                return false;
            }

            exchange.setResource(this.resourceMappings.getSite(uri));
        }

        else {

            exchange.setResource(this.resourceMappings.getDashboard(uri));
        }

        for(final String illegalElement : this.illegalPathContains) {

            if(uri.contains(illegalElement)) {

                exchange.setUriBad(true);
                break;
            }
        }

        if(exchange.getMethod() == HttpMethod.OPTIONS) { // TODO: move down into handler (CORS is specific by path in theory)

            super.respond(exchange, HttpStatus.NO_CONTENT, this.siteCorsHeaders);
            return false;
        }

        return true;
    }

    ///..
    private boolean doAuthorizationFilter(final HttpExchange exchange, final boolean isDashboard) {

        final Resource resource = exchange.getResource();

        if(isDashboard) {

            if(resource == null) {

                return true;
            }

            else {

                if(resource.getAuthorizationAction() == AuthorizationAction.ALLOW) {

                    return true;
                }

                else {

                    final SecurityFailure securityFailure = this.sessionService.isAllowed(exchange);

                    if(securityFailure != null) {

                        super.respond(exchange, HttpStatus.SEE_OTHER, ApplicationProperties.LOGIN_REDIRECT_HEADERS);
                        return false;
                    }

                    return true;
                }
            }
        }

        else {

            
        }

        /*final Resource resource = exchange.getResource();
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

                    exchange.requestClose();
                    super.respond(exchange, HttpStatus.UNAUTHORIZED, MimeType.TEXT, securityFailure.getMessage());
                }

                return false;
            }
        }

        return true;*/
    }

    ///..
    private void replenishTask() {

        for(final Entry<IpAddress, RateLimitEntry> entry : this.rateLimitTracker.entrySet()) {

            if(entry.getValue().replenish(this.rateLimitAmount)) this.rateLimitTracker.remove(entry.getKey());
        }
    }

    ///
}
