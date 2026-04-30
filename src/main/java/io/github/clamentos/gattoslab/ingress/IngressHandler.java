package io.github.clamentos.gattoslab.ingress;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exceptions.handling.GlobalExceptionHandler;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpUtils;
import io.github.clamentos.gattoslab.ingress.filters.BlacklistFilter;
import io.github.clamentos.gattoslab.ingress.filters.CorsFilter;
import io.github.clamentos.gattoslab.ingress.filters.SecurityFilter;
import io.github.clamentos.gattoslab.ingress.filters.ratelimit.RateLimitFilter;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.SquashedLogsContainer;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.session.SessionRole;
import io.github.clamentos.gattoslab.session.SessionService;
import io.github.clamentos.gattoslab.website.Website;

///..
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

///..
import lombok.extern.slf4j.Slf4j;

///
@Slf4j

///
public final class IngressHandler implements HttpHandler {

    ///
    private final BlacklistFilter blacklistFilter;
    private final CorsFilter corsFilter;
    private final RateLimitFilter rateLimitFilter;
    private final SecurityFilter securityFilter;

    ///..
    private final ObservabilityService observabilityService;
    private final RequestDispatcher requestDispatcher;
    private final GlobalExceptionHandler globalExceptionHandler;

    ///
    public IngressHandler(

        final ApplicationProperties applicationProperties,
        final BlacklistFilter blacklistFilter,
        final BatchScheduler batchScheduler,
        final SquashedLogsContainer squashedLogsContainer,
        final SessionService sessionService,
        final ObservabilityService observabilityService,
        final RequestDispatcher requestDispatcher,
        final GlobalExceptionHandler globalExceptionHandler,
        final Website website
    ) {

        final boolean isCorsEnabled = applicationProperties.isCorsEnabled();
        final boolean isRateLimitEnabled = applicationProperties.isRateLimitEnabled();
        final boolean isSecurityEnabled = applicationProperties.isSessionsEnabled();

        this.blacklistFilter = blacklistFilter;
        corsFilter = isCorsEnabled ? new CorsFilter(applicationProperties) : null;
        rateLimitFilter = isRateLimitEnabled ? new RateLimitFilter(applicationProperties, batchScheduler, squashedLogsContainer) : null;
        securityFilter = isSecurityEnabled ? new SecurityFilter(applicationProperties, SessionRole.ADMIN, sessionService, website) : null;

        this.observabilityService = observabilityService;
        this.requestDispatcher = requestDispatcher;
        this.globalExceptionHandler = globalExceptionHandler;
    }

    ///
    @Override
    public void handleRequest(final HttpServerExchange exchange) {

        exchange.putAttachment(HttpUtils.START_TIME_EPOCH_MS, System.currentTimeMillis());

        try {

            blacklistFilter.isAllowed(exchange);
            exchange.putAttachment(HttpUtils.DECODED_HTTP_METHOD, HttpMethod.decode(exchange.getRequestMethod()));

            if(corsFilter != null && corsFilter.isAllowed(exchange)) {

                if(rateLimitFilter != null) rateLimitFilter.rateLimit(exchange);
                if(securityFilter != null) securityFilter.authorize(exchange);
                if(requestDispatcher.dispatch(exchange)) observabilityService.updateRequestMetrics(exchange);
            }
        }

        catch(final Exception exc) {

            globalExceptionHandler.handle(exc, exchange);
            observabilityService.updateRequestMetrics(exchange);
        }
    }

    ///
}
