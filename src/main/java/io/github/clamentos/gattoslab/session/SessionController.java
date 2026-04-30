package io.github.clamentos.gattoslab.session;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.http.HttpUtils;
import io.github.clamentos.gattoslab.http.MimeType;

///..
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.Cookie;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

///..
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

///
public final class SessionController {

    ///
    private final String cookieName;
    private final Map<SessionRole, String> cookieAttributes;

    ///..
    private final SessionService sessionService;

    ///..
    public SessionController(final ApplicationProperties applicationProperties, final SessionService sessionService) {

        cookieName = applicationProperties.getSessionsCookieName();
        cookieAttributes = new EnumMap<>(SessionRole.class);

        for(final SessionRole role : SessionRole.values()) {

            cookieAttributes.put(role, sessionService.getCookieAttributes(role) + " Max-Age=" + applicationProperties.getSessionsDuration() + ";");
        }

        this.sessionService = sessionService;
    }

    ///
    public void createSession(final HttpServerExchange exchange) throws ApiSecurityException {

        final SessionRole role = SessionRole.fromParam(exchange);
        final Cookie cookie = exchange.getRequestCookie(cookieName + role.name());
        final HeaderMap headers = exchange.getRequestHeaders();

        final Entry<String, SessionMetadata> session = sessionService.createSession(

            HttpUtils.getHeaderValue(headers, Headers.AUTHORIZATION_STRING),
            role,
            cookie != null ? cookie.getValue() : null,
            exchange.getSourceAddress().getAddress(),
            HttpUtils.getHeaderValue(headers, Headers.USER_AGENT_STRING)
        );

        this.respondWithCookie(exchange, role, session);
    }

    ///..
    public void refreshSession(final HttpServerExchange exchange) throws ApiSecurityException {

        final SessionRole role = SessionRole.fromParam(exchange);
        final Cookie cookie = exchange.getRequestCookie(cookieName + role.name());

        final Entry<String, SessionMetadata> session = sessionService.refreshSession(

            cookie != null ? cookie.getValue() : null,
            role,
            exchange.getSourceAddress().getAddress(),
            HttpUtils.getHeaderValue(exchange.getRequestHeaders(), Headers.USER_AGENT_STRING)
        );

        this.respondWithCookie(exchange, role, session);
    }

    ///..
    public void deleteSession(final HttpServerExchange exchange) throws ApiSecurityException {

        final SessionRole role = SessionRole.fromParam(exchange);
        final Cookie cookie = exchange.getRequestCookie(cookieName + role.name());

        sessionService.deleteSession(cookie != null ? cookie.getValue() : null, role);
    }

    ///.
    private void respondWithCookie(final HttpServerExchange exchange, final SessionRole role, final Entry<String, SessionMetadata> session) {

        final HeaderMap headers = exchange.getResponseHeaders();

        HttpUtils.addContentType(headers, MimeType.TXT);
        HttpUtils.addCookie(headers, cookieName + role.name() + cookieAttributes.get(role).replace("$", session.getKey()));

        exchange.setStatusCode(StatusCodes.OK);
        exchange.getResponseSender().send(Long.toString(session.getValue().getExpiresAt()));
    }

    ///
}
