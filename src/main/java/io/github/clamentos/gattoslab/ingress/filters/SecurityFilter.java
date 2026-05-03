package io.github.clamentos.gattoslab.ingress.filters;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.exceptions.RedirectException;
import io.github.clamentos.gattoslab.http.HttpUtils;
import io.github.clamentos.gattoslab.session.SessionRole;
import io.github.clamentos.gattoslab.session.SessionService;
import io.github.clamentos.gattoslab.utils.GenericUtils;
import io.github.clamentos.gattoslab.website.Apis;
import io.github.clamentos.gattoslab.website.Website;

///..
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.Headers;

///..
import java.util.Set;
import java.util.stream.Collectors;

///..
import lombok.extern.slf4j.Slf4j;

///
@Slf4j

///
public final class SecurityFilter {

    ///
    private static final String SOURCE_AUTHORIZE = "SecurityFilter.authorize";

    ///
    private final String cookieName;
    private final SessionRole roleToCheck;
    private final Set<String> protectedPaths;

    ///..
    private final SessionService sessionService;

    ///
    public SecurityFilter(final ApplicationProperties applicationProperties, final SessionRole roleToCheck, final SessionService sessionService, final Website website) {

        this.roleToCheck = roleToCheck;
        cookieName = applicationProperties.getSessionsCookieName() + roleToCheck.name();
        protectedPaths = website.getPaths().stream().filter(p -> p.startsWith("/admin")).collect(Collectors.toSet());

        this.sessionService = sessionService;
    }

    ///
    public void authorize(final HttpServerExchange exchange) throws ApiSecurityException, RedirectException {

        final String path = exchange.getRequestPath();
        if(roleToCheck == SessionRole.ADMIN && !protectedPaths.contains(path)) return;

        final Cookie cookie = exchange.getRequestCookie(cookieName);
        String errorMessage = "";

        if(cookie != null) {

            final String fingerprint = GenericUtils.composeFingerprint(

                exchange.getSourceAddress().getAddress(),
                HttpUtils.getHeaderValue(exchange.getRequestHeaders(), Headers.USER_AGENT_STRING)
            );

            if(sessionService.check(roleToCheck, cookie.getValue(), fingerprint) == null) errorMessage += "Invalid, expired or non existent session";
            else return;
        }

        else {

            errorMessage += "No " + roleToCheck + " session cookie found in the request";
        }

        if(path.endsWith(".html") && roleToCheck == SessionRole.ADMIN) throw new RedirectException(Apis.FE_LOGIN, SOURCE_AUTHORIZE);
        else throw new ApiSecurityException(errorMessage, SOURCE_AUTHORIZE);
    }

    ///
}
