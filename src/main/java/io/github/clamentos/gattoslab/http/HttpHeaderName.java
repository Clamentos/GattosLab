package io.github.clamentos.gattoslab.http;

///
import java.util.Locale;

///..
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter

///
public enum HttpHeaderName {

    ///
    ACCESS_CONTROL_ALLOW_CREDENTIALS("Access-Control-Allow-Credentials: "),
    ACCESS_CONTROL_ALLOW_METHODS("Access-Control-Allow-Methods: "),
    ACCESS_CONTROL_ALLOW_ORIGIN("Access-Control-Allow-Origin: "),
    ACCESS_CONTROL_MAX_AGE("Access-Control-Max-Age: "),
    AUTHORIZATION("Authorization: "),
    CACHE_CONTROL("Cache-Control: "),
    CLEAR_SITE_DATA("Clear-Site-Data: "),
    CONTENT_ENCODING("Content-Encoding: "),
    CONTENT_LENGTH("Content-Length: "),
    CONTENT_SECURITY_POLICY("Content-Security-Policy: "),
    CONTENT_TYPE("Content-Type: "),
    COOKIE("Cookie: "),
    CONNECTION("Connection: "),
    DATE("Date: "),
    KEEP_ALIVE("Keep-Alive: "),
    LOCATION("Location: "),
    RETRY_AFTER("Retry-After: "),
    SET_COOKIE("Set-Cookie: "),
    STRICT_TRANSPORT_SECURITY("Strict-Transport-Security: "),
    TRANSFER_ENCODING("Transfer-Encoding: "),
    USER_AGENT("User-Agent: "),
    X_FRAME_OPTIONS("X-Frame-Options: ");

    ///
    private final String valueForResponse;

    ///
    public static HttpHeaderName decode(final String value) {

        switch(value.toLowerCase(Locale.US)) {

            case "authorization": return HttpHeaderName.AUTHORIZATION;
            case "content-length": return HttpHeaderName.CONTENT_LENGTH;
            case "cookie": return HttpHeaderName.COOKIE;
            case "connection": return HttpHeaderName.CONNECTION;
            case "transfer-encoding": return HttpHeaderName.TRANSFER_ENCODING;
            case "user-agent": return HttpHeaderName.USER_AGENT;

            default: return null;
        }
    }

    ///
}
