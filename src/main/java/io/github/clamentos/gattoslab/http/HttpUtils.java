package io.github.clamentos.gattoslab.http;

///
import io.undertow.server.HttpServerExchange;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

///..
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
    public static final AttachmentKey<HttpMethod> DECODED_HTTP_METHOD = AttachmentKey.create(HttpMethod.class);
    public static final AttachmentKey<Boolean> BROKEN_PIPE = AttachmentKey.create(Boolean.class);

    ///..
    public static HeaderMap addContentType(final HeaderMap headerMap, final MimeType mimeType) {

        return headerMap.add(HttpString.tryFromString(Headers.CONTENT_TYPE_STRING), mimeType.getMimeValue());
    }

    ///..
    public static HeaderMap addJsonContentType(final HeaderMap headerMap) {

        return headerMap.add(HttpString.tryFromString(Headers.CONTENT_TYPE_STRING), MimeType.JSON.getMimeValue());
    }

    ///..
    public static HeaderMap addGzipEncoding(final HeaderMap headerMap) {

        return headerMap.add(HttpString.tryFromString(Headers.CONTENT_ENCODING_STRING), "gzip");
    }

    ///..
    public static HeaderMap addCache(final HeaderMap headerMap, final String duration) {

        return headerMap.add(HttpString.tryFromString(Headers.CACHE_CONTROL_STRING), "max-age=" + duration + ", public");
    }

    ///..
    public static HeaderMap addNoCache(final HeaderMap headerMap) {

        return headerMap.add(HttpString.tryFromString(Headers.CACHE_CONTROL_STRING), "no-cache");
    }

    ///..
    public static HeaderMap addLastModified(final HeaderMap headerMap, final String lastModified) {

        return headerMap.add(HttpString.tryFromString(Headers.LAST_MODIFIED_STRING), lastModified);
    }

    ///..
    public static HeaderMap addRetryAfter(final HeaderMap headerMap, final String retryAfterStr) {

        return headerMap.add(HttpString.tryFromString(Headers.RETRY_AFTER_STRING), retryAfterStr);
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
    public static HeaderMap addCorsMaxAge(final HeaderMap headerMap, final String age) {

        return headerMap.add(HttpString.tryFromString("Access-Control-Max-Age"), age);
    }

    ///..
    public static HeaderMap addRedirect(final HeaderMap headerMap, final String location) {

        return headerMap.add(HttpString.tryFromString(Headers.LOCATION_STRING), location);
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
    public static void respondRest(final HttpServerExchange exchange, final int statusCode, final String json, final HeaderMap extraHeaders) {

        final HeaderMap headers = clearHeaders(exchange);

        HttpUtils.addJsonContentType(headers);
        HttpUtils.addNoCache(headers);

        if(extraHeaders != null) headers.putAll(extraHeaders);

        exchange.setStatusCode(statusCode);
        exchange.getResponseSender().send(json);
    }

    ///
}
