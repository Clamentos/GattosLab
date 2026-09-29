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
    DATE("Date: ".getBytes()),
    KEEP_ALIVE("Keep-Alive: ".getBytes()),
    LOCATION("Location: ".getBytes()),
    RETRY_AFTER("Retry-After: ".getBytes()),
    SET_COOKIE("Set-Cookie: ".getBytes()),
    STRICT_TRANSPORT_SECURITY("Strict-Transport-Security: ".getBytes()),
    TRANSFER_ENCODING("Transfer-Encoding: ".getBytes()),
    USER_AGENT("User-Agent: ".getBytes()),
    X_FRAME_OPTIONS("X-Frame-Options: ".getBytes());

    ///
    private final byte[] valueForResponse;

    ///
    public static HttpHeader decode(final String value) {

        switch(value.toLowerCase(Locale.US)) {

            case "authorization": return HttpHeader.AUTHORIZATION;
            case "content-length": return HttpHeader.CONTENT_LENGTH;
            case "cookie": return HttpHeader.COOKIE;
            case "connection": return HttpHeader.CONNECTION;
            case "transfer-encoding": return HttpHeader.TRANSFER_ENCODING;
            case "user-agent": return HttpHeader.USER_AGENT;

            default: return null;
        }
    }

    ///
}
