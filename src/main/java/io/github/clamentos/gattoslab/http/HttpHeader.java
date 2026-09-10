package io.github.clamentos.gattoslab.http;

///
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter

///
public enum HttpHeader {

    ///
    ACCESS_CONTROL_ALLOW_CREDENTIALS("Access-Control-Allow-Credentials"),
    ACCESS_CONTROL_ALLOW_METHODS("Access-Control-Allow-Methods"),
    ACCESS_CONTROL_ALLOW_ORIGIN("Access-Control-Allow-Origin"),
    ACCESS_CONTROL_MAX_AGE("Access-Control-Max-Age"),
    ACCESS_CONTROL_REQUEST_METHOD("Access-Control-Request-Method"),
    AUTHORIZATION("Authorization"),
    CACHE_CONTROL("Cache-Control"),
    CLEAR_SITE_DATA("Clear-Site-Data"),
    CONTENT_ENCODING("Content-Encoding"),
    CONTENT_LENGTH("Content-Length"),
    CONTENT_SECURITY_POLICY("Content-Security-Policy"),
    CONTENT_TYPE("Content-Type"),
    COOKIE("Cookie"),
    LOCATION("Location"),
    RETRY_AFTER("Retry-After"),
    SET_COOKIE("Set-Cookie"),
    STRICT_TRANSPORT_SECURITY("Strict-Transport-Security"),
    USER_AGENT("User-Agent"),
    X_FRAME_OPTIONS("X-Frame-Options"),
    X_FORWARDED_FOR("X-Forwarded-For");

    ///
    private final String name;

    ///
}
