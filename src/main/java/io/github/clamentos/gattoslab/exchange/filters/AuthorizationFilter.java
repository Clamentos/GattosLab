package io.github.clamentos.gattoslab.exchange.filters;

///
import com.sun.net.httpserver.HttpExchange;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exchange.Responder;
import io.github.clamentos.gattoslab.exchange.filters.components.AuthorizationAction;
import io.github.clamentos.gattoslab.exchange.handling.components.Resource;
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.security.SecurityFailure;
import io.github.clamentos.gattoslab.security.SessionService;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.util.Map;

///
public final class AuthorizationFilter extends Responder {

    ///
    private final SquashingLogger squashingLogger;
    private final SessionService sessionService;

    ///
    public AuthorizationFilter(

        final ObservabilityService observabilityService,
        final SquashingLogger squashingLogger,
        final SessionService sessionService
    ) {

        super(observabilityService);

        this.squashingLogger = squashingLogger;
        this.sessionService = sessionService;
    }

    ///
    public boolean isOk(final HttpExchange exchange) {

        final Resource resource = (Resource)exchange.getAttribute(ApplicationProperties.REQUEST_RESOURCE_ATTRIBUTE);
        if(resource == null) return true;

        final AuthorizationAction authorizationAction = resource.getAuthorizationAction();

        if(authorizationAction != AuthorizationAction.ALLOW) {

            final SecurityFailure securityFailure = this.sessionService.isAllowed(exchange.getRequestHeaders().get(HttpHeader.COOKIE.getName()));

            if(securityFailure != null) {

                if(authorizationAction == AuthorizationAction.REDIRECT) {

                    super.respond(exchange, HttpStatus.SEE_OTHER, ApplicationProperties.LOGIN_REDIRECT_HEADERS);
                }

                else {

                    this.squashingLogger.warning("Authorization failed for '" + GenericUtils.composeFingerprint(exchange) + " # times");

                    super.respond(exchange, HttpStatus.UNAUTHORIZED, Map.of(), MimeType.TEXT, securityFailure.getMessage());
                    exchange.close();
                }

                return false;
            }
        }

        return true;
    }

    ///
}
