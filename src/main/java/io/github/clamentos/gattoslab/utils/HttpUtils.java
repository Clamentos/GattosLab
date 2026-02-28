package io.github.clamentos.gattoslab.utils;

///
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

///..
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;

///..
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

///
@NoArgsConstructor(access = AccessLevel.PRIVATE)

///
public final class HttpUtils {

    ///
    public static final AttachmentKey<Long> START_TIME_EPOCH_MS = AttachmentKey.create(Long.class);

    ///..
    public static HeaderMap addContentType(final HeaderMap headerMap, final MimeType mimeType) {

        return headerMap.add(HttpString.tryFromString(Headers.CONTENT_TYPE_STRING), mimeType.getMimeValue());
    }

    ///..
    public static HeaderMap addJsonContent(final HeaderMap headerMap) {

        return headerMap.add(HttpString.tryFromString(Headers.CONTENT_TYPE_STRING), MimeType.JSON.getMimeValue());
    }

    ///..
    public static HeaderMap addGzipEncoding(final HeaderMap headerMap) {

        return headerMap.add(HttpString.tryFromString(Headers.CONTENT_ENCODING_STRING), "gzip");
    }

    ///..
    public static HeaderMap addCache(final HeaderMap headerMap, final int duration) {

        return headerMap.add(HttpString.tryFromString(Headers.CACHE_CONTROL_STRING), "max-age=" + Integer.toString(duration) + ", public");
    }

    ///..
    public static HeaderMap addNoCache(final HeaderMap headerMap) {

        return headerMap.add(HttpString.tryFromString(Headers.CACHE_CONTROL_STRING), "no-cache");
    }

    ///..
    public static HeaderMap addLastModified(final HeaderMap headerMap, final OffsetDateTime lastModified) {

        return headerMap.add(HttpString.tryFromString(Headers.LAST_MODIFIED_STRING), DateTimeFormatter.RFC_1123_DATE_TIME.format(lastModified));
    }

    ///..
    public static HeaderMap addRetryAfter(final HeaderMap headerMap, final int retryAfter) {

        return headerMap.add(HttpString.tryFromString(Headers.RETRY_AFTER_STRING), Integer.toString(retryAfter));
    }

    ///..
    public static HeaderMap addCookie(final HeaderMap headerMap, final String cookie) {

        return headerMap.add(HttpString.tryFromString(Headers.SET_COOKIE_STRING), cookie);
    }

    ///..
    public static HeaderMap addAllowedMethods(final HeaderMap headerMap, final Set<HttpMethod> allowedMethods) {

        return headerMap.add(

            HttpString.tryFromString("Access-Control-Allow-Methods"),
            allowedMethods.stream().map(HttpMethod::name).collect(Collectors.joining(", "))
        );
    }

    ///..
    public static HeaderMap addAllowedOrigins(final HeaderMap headerMap, final Set<String> allowedOrigins) {

        return headerMap.add(HttpString.tryFromString("Access-Control-Allow-Origin"), allowedOrigins.stream().collect(Collectors.joining(", ")));
    }

    ///..
    public static HeaderMap addAllowCredentials(final HeaderMap headerMap) {

        return headerMap.add(HttpString.tryFromString("Access-Control-Allow-Credentials"), "true");
    }

    ///..
    public static HeaderMap addCorsMaxAge(final HeaderMap headerMap, final int age) {

        return headerMap.add(HttpString.tryFromString("Access-Control-Max-Age"), Integer.toString(age));
    }

    ///..
    public static String getHeaderValue(final HeaderMap headerMap, final String name) {

        final HeaderValues values = headerMap.get(name);
        if(values == null) return null;

        return values.peekFirst();
    }

    ///..
    public static HeaderMap clearHeaders(final HttpServerExchange exchange) {

        final HeaderMap headers = exchange.getResponseHeaders();
        headers.clear();

        return headers;
    }

    ///..
    public static void respondRest(final HttpServerExchange exchange, final int statusCode, final String json, final HeaderMap additionalHeaders)
    throws IllegalStateException {

        final HeaderMap headers = clearHeaders(exchange);

        HttpUtils.addJsonContent(headers);
        HttpUtils.addNoCache(headers);

        if(additionalHeaders != null) headers.putAll(additionalHeaders);

        exchange.setStatusCode(statusCode);
        exchange.getResponseSender().send(json);
    }

    ///
}
