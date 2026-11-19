package io.github.clamentos.gattoslab.server.http.exchange.components.resources;

///
import io.github.clamentos.gattoslab.security.components.AuthorizationAction;

///..
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter

///
public enum Api implements Resource {

    ///
    LOGIN("/api/authentication/login", AuthorizationAction.ALLOW),
    LOGOUT("/api/authentication/logout", AuthorizationAction.ALLOW),
    GET_SESSIONS("/api/authentication/sessions", AuthorizationAction.REDIRECT),
    GET_LOGS("/api/observability/logs", AuthorizationAction.REDIRECT),
    GET_REQUEST_METRICS("/api/observability/request-metrics", AuthorizationAction.REDIRECT),
    GET_SYSTEM_METRICS("/api/observability/system-metrics", AuthorizationAction.REDIRECT),
    GET_CRAWL_METRICS("/api/observability/crawl-metrics", AuthorizationAction.REDIRECT);

    ///
    private final String path;
    private final AuthorizationAction authorizationAction;

    ///
    public static Api decode(final String value) {

        switch(value) {

            case "/api/authentication/login": return Api.LOGIN;
            case "/api/authentication/logout": return Api.LOGOUT;
            case "/api/authentication/sessions": return Api.GET_SESSIONS;
            case "/api/observability/logs": return Api.GET_LOGS;
            case "/api/observability/request-metrics": return Api.GET_REQUEST_METRICS;
            case "/api/observability/system-metrics": return Api.GET_SYSTEM_METRICS;
            case "/api/observability/crawl-metrics": return Api.GET_CRAWL_METRICS;

            default: return null;
        }
    }

    ///
}
