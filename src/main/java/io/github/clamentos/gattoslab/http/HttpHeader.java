package io.github.clamentos.gattoslab.http;

///
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter

///
public enum HttpHeader {

    ///
    ACCESS_CONTROL_ALLOW_CREDENTIALS("Access-Control-Allow-Credentials: ".getBytes()),
    ACCESS_CONTROL_ALLOW_METHODS("Access-Control-Allow-Methods: ".getBytes()),
    ACCESS_CONTROL_ALLOW_ORIGIN("Access-Control-Allow-Origin: ".getBytes()),
    ACCESS_CONTROL_MAX_AGE("Access-Control-Max-Age: ".getBytes()),
    AUTHORIZATION("Authorization: ".getBytes()),
    CACHE_CONTROL("Cache-Control: ".getBytes()),
    CLEAR_SITE_DATA("Clear-Site-Data: ".getBytes()),
    CONTENT_ENCODING("Content-Encoding: ".getBytes()),
    CONTENT_LENGTH("Content-Length: ".getBytes()),
    CONTENT_SECURITY_POLICY("Content-Security-Policy: ".getBytes()),
    CONTENT_TYPE("Content-Type: ".getBytes()),
    COOKIE("Cookie: ".getBytes()),
    CONNECTION("Connection: ".getBytes()),
    LOCATION("Location: ".getBytes()),
    RETRY_AFTER("Retry-After: ".getBytes()),
    SET_COOKIE("Set-Cookie: ".getBytes()),
    STRICT_TRANSPORT_SECURITY("Strict-Transport-Security: ".getBytes()),
    TRANSFER_ENCODING("Transfer-Encoding: ".getBytes()),
    USER_AGENT("User-Agent: ".getBytes()),
    X_FRAME_OPTIONS("X-Frame-Options: ".getBytes());

    ///
    public static HttpHeader decode(final String value, final StringBuilder buffer) {

        switch(GenericUtils.fastToLower(value, buffer)) {

            case "authorization": return AUTHORIZATION;
            case "cache-control": return CACHE_CONTROL;
            case "content-encoding": return CONTENT_ENCODING;
            case "content-length": return CONTENT_LENGTH;
            case "content-type": return CONTENT_TYPE;
            case "cookie": return COOKIE;
            case "connection": return CONNECTION;
            case "transfer-encoding": return TRANSFER_ENCODING;
            case "user-agent": return USER_AGENT;

            default: return null;
        }
    }

    ///
    private final byte[] valueForResponse;

    ///
}
