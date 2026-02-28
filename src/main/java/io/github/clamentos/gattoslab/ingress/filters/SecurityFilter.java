package io.github.clamentos.gattoslab.ingress.filters;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.exceptions.RedirectException;
import io.github.clamentos.gattoslab.session.SessionRole;
import io.github.clamentos.gattoslab.session.SessionService;
import io.github.clamentos.gattoslab.utils.GenericUtils;
import io.github.clamentos.gattoslab.utils.HttpUtils;

///..
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.Headers;

///..
import lombok.extern.slf4j.Slf4j;

///
@Slf4j

///
public final class SecurityFilter {

    ///
    private final String cookieName;
    private final SessionRole roleToCheck;

    ///..
    private final SessionService sessionService;

    ///
    public SecurityFilter(final ApplicationProperties applicationProperties, final SessionRole roleToCheck, final SessionService sessionService) {

        this.roleToCheck = roleToCheck;
        cookieName = applicationProperties.getSessionConfig().getCookieName() + roleToCheck.name();

        this.sessionService = sessionService;
    }

    ///
    public void authorize(final HttpServerExchange exchange) throws ApiSecurityException, RedirectException {

        final String path = exchange.getRequestPath();
        if(roleToCheck == SessionRole.ADMIN && !path.startsWith("/admin")) return;

        final Cookie cookie = exchange.getRequestCookie(cookieName);

        if(cookie != null) {

            final String fingerprint = GenericUtils.composeFingerprint(

                exchange.getSourceAddress().getAddress(),
                HttpUtils.getHeaderValue(exchange.getRequestHeaders(), Headers.USER_AGENT_STRING)
            );

            if(sessionService.check(roleToCheck, cookie.getValue(), fingerprint) == null) {

                if(path.endsWith(".html") && roleToCheck == SessionRole.ADMIN) throw new RedirectException("/login.html");
                else throw new ApiSecurityException("Invalid, expired or non existent session");
            }
        }

        else {

            if(path.endsWith(".html") && roleToCheck == SessionRole.ADMIN) throw new RedirectException("/login.html");
            else throw new ApiSecurityException("No " + roleToCheck + " session cookie found in the request");
        }
    }

    ///
}
