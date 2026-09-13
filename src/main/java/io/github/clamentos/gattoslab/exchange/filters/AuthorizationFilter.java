package io.github.clamentos.gattoslab.exchange.filters;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exchange.Responder;
import io.github.clamentos.gattoslab.exchange.filters.components.AuthorizationAction;
import io.github.clamentos.gattoslab.exchange.handling.components.Resource;
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.http.server.Filter;
import io.github.clamentos.gattoslab.http.server.HttpExchange;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.observability.logging.SquashingLogger;
import io.github.clamentos.gattoslab.security.SecurityFailure;
import io.github.clamentos.gattoslab.security.SessionService;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.util.List;
import java.util.Map;

///
public final class AuthorizationFilter extends Responder implements Filter {

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
    @Override
    public boolean filter(final HttpExchange exchange) {

        final Resource resource = exchange.getResource();
        if(resource == null) return true;

        final AuthorizationAction authorizationAction = resource.getAuthorizationAction();

        if(authorizationAction != AuthorizationAction.ALLOW) {

            final List<String> cookies = GenericUtils.fastSplit(exchange.getRequestHeaders().get(HttpHeader.COOKIE), ';');
            final SecurityFailure securityFailure = this.sessionService.isAllowed(cookies);

            if(securityFailure != null) {

                if(authorizationAction == AuthorizationAction.REDIRECT) {

                    super.respond(exchange, HttpStatus.SEE_OTHER, ApplicationProperties.LOGIN_REDIRECT_HEADERS);
                }

                else {

                    this.squashingLogger.warning(GenericUtils.composeMessageForSquash("Authorization failed for", exchange));
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
