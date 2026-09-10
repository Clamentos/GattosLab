package io.github.clamentos.gattoslab.exchange.handling.components;

///
import io.github.clamentos.gattoslab.exchange.filters.components.AuthorizationAction;

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
    GET_SESSIONS("/api/authentication/sessions", AuthorizationAction.BLOCK),

    GET_LOGS("/api/observability/logs", AuthorizationAction.BLOCK),
    GET_REQUEST_METRICS("/api/observability/request-metrics", AuthorizationAction.BLOCK),
    GET_SYSTEM_METRICS("/api/observability/system-metrics", AuthorizationAction.BLOCK),
    GET_CRAWL_METRICS("/api/observability/crawl-metrics", AuthorizationAction.BLOCK);

    ///
    private final String path;
    private final AuthorizationAction authorizationAction;

    ///
}
