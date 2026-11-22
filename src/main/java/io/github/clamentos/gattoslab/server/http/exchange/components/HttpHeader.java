package io.github.clamentos.gattoslab.server.http.exchange.components;

///
import java.nio.charset.StandardCharsets;

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
    ACCESS_CONTROL_ALLOW_CREDENTIALS("Access-Control-Allow-Credentials: ".getBytes(StandardCharsets.ISO_8859_1), false),
    ACCESS_CONTROL_ALLOW_METHODS("Access-Control-Allow-Methods: ".getBytes(StandardCharsets.ISO_8859_1), false),
    ACCESS_CONTROL_ALLOW_ORIGIN("Access-Control-Allow-Origin: ".getBytes(StandardCharsets.ISO_8859_1), false),
    ACCESS_CONTROL_MAX_AGE("Access-Control-Max-Age: ".getBytes(StandardCharsets.ISO_8859_1), false),
    AUTHORIZATION("Authorization: ".getBytes(StandardCharsets.ISO_8859_1), true),
    CACHE_CONTROL("Cache-Control: ".getBytes(StandardCharsets.ISO_8859_1), false),
    CLEAR_SITE_DATA("Clear-Site-Data: ".getBytes(StandardCharsets.ISO_8859_1), false),
    CONTENT_ENCODING("Content-Encoding: ".getBytes(StandardCharsets.ISO_8859_1), false),
    CONTENT_LENGTH("Content-Length: ".getBytes(StandardCharsets.ISO_8859_1), false),
    CONTENT_SECURITY_POLICY("Content-Security-Policy: ".getBytes(StandardCharsets.ISO_8859_1), false),
    CONTENT_TYPE("Content-Type: ".getBytes(StandardCharsets.ISO_8859_1), false),
    CONNECTION("Connection: ".getBytes(StandardCharsets.ISO_8859_1), false),
    COOKIE("Cookie: ".getBytes(StandardCharsets.ISO_8859_1), true),
    DATE("Date: ".getBytes(StandardCharsets.ISO_8859_1), false),
    HOST("Host: ".getBytes(StandardCharsets.ISO_8859_1), false),
    KEEP_ALIVE("Keep-Alive: ".getBytes(StandardCharsets.ISO_8859_1), false),
    LOCATION("Location: ".getBytes(StandardCharsets.ISO_8859_1), false),
    RETRY_AFTER("Retry-After: ".getBytes(StandardCharsets.ISO_8859_1), false),
    SET_COOKIE("Set-Cookie: ".getBytes(StandardCharsets.ISO_8859_1), true),
    STRICT_TRANSPORT_SECURITY("Strict-Transport-Security: ".getBytes(StandardCharsets.ISO_8859_1), false),
    TRANSFER_ENCODING("Transfer-Encoding: ".getBytes(StandardCharsets.ISO_8859_1), false),
    USER_AGENT("User-Agent: ".getBytes(StandardCharsets.ISO_8859_1), false),
    X_FRAME_OPTIONS("X-Frame-Options: ".getBytes(StandardCharsets.ISO_8859_1), false),

    ///..
    FILTER(null, false);

    ///
    private final byte[] responseValue;
    private final boolean isSensitive;

    ///
    public static HttpHeader decode(final String value) {

        switch(value) {

            case "authorization": return HttpHeader.AUTHORIZATION;
            case "content-length": return HttpHeader.CONTENT_LENGTH;
            case "cookie": return HttpHeader.COOKIE;
            case "connection": return HttpHeader.CONNECTION;
            case "content-encoding": return HttpHeader.CONTENT_ENCODING;
            case "content-type": return HttpHeader.CONTENT_TYPE;
            case "filter": return HttpHeader.FILTER;
            case "host": return HttpHeader.HOST;
            case "transfer-encoding": return HttpHeader.TRANSFER_ENCODING;
            case "user-agent": return HttpHeader.USER_AGENT;

            default: return null;
        }
    }

    ///
}
