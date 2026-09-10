package io.github.clamentos.gattoslab.utils;

///
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.http.BodyStreamer;
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;

///..
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

///..
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

///
@NoArgsConstructor(access = AccessLevel.PRIVATE)

///
public final class GenericUtils {

    ///
    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();

    ///..
    private static final String[] DATA_SIZE_UNITS = {"KiB", "MiB", "GiB", "TiB", "PiB", "EiB"};

    ///.
    public static String composeFingerprint(final HttpExchange exchange) {

        final StringJoiner joiner = new StringJoiner(ApplicationProperties.FINGERPRINT_SEPARATOR);
        final InetSocketAddress socketAddress = exchange.getRemoteAddress();

        if(socketAddress != null) {

            final InetAddress address = socketAddress.getAddress();
            if(address != null) joiner.add(address.getHostAddress());
        }

        else {

            joiner.add("null");
        }

        joiner.add(normalizedForObservability(exchange.getRequestHeaders().getFirst(HttpHeader.USER_AGENT.getName())));
        return joiner.toString();
    }

    ///..
    public static void silentSleep(final long amount) {

        try {

            Thread.sleep(amount);
        }

        catch(final InterruptedException _) {

            Thread.currentThread().interrupt();
        }
    }

    ///..
    public static Thread spawnVirtualThread(final String name, final Runnable task) {

        return Thread.ofVirtual().name(name).start(task);
    }

    ///..
    public static Thread createVirtualThread(final String name, final Runnable task) {

        return Thread.ofVirtual().name(name).unstarted(task);
    }

    ///..
    public static List<String> fastSplit(final String input, final char delimiter) {

        if(input == null) return null;
        if(input.isEmpty()) return List.of();

        final int length = input.length();
        final List<String> splits = new ArrayList<>(8);
        final StringBuilder stringBuilder = new StringBuilder(length);

        char currentChar = 0;

        for(int i = 0; i < length; i++) {

            currentChar = input.charAt(i);
            if(currentChar != delimiter) stringBuilder.append(currentChar);

            else {

                splits.add(stringBuilder.toString());
                stringBuilder.setLength(0);
            }
        }

        splits.add(stringBuilder.toString());
        return splits;
    }

    ///..
    public static CharSequence normalizedForObservability(final String input) {

        if(input == null) return null;
        if(input.isEmpty()) return "";

        final int length = input.length();
        final StringBuilder stringBuilder = new StringBuilder(length);
        char currentChar;

        for(int i = 0; i < length; i++) {

            currentChar = input.charAt(i);

            if(currentChar == '\n') stringBuilder.append(ApplicationProperties.NEWLINE_REPLACEMENT);
            else if(currentChar == ApplicationProperties.FIELD_SEPARATOR) stringBuilder.append(ApplicationProperties.FIELD_SEPARATOR_REPLACEMENT);
            else if(currentChar > 127) stringBuilder.append(ApplicationProperties.NON_ASCII_REPLACEMENT);
            else stringBuilder.append(currentChar);
        }

        return stringBuilder;
    }

    ///..
    public static void respondSimple(

        final HttpExchange exchange,
        final HttpStatus status,
        final Map<String, List<String>> headers,
        final MimeType mimeType,
        final Object body

    ) throws IOException {

        exchange.setAttribute(ApplicationProperties.REQUEST_HANDLED_ATTRIBUTE, false);
        final Headers responseHeaders = exchange.getResponseHeaders();

        responseHeaders.putAll(ApplicationProperties.EXTRA_HEADERS);
        responseHeaders.putAll(ApplicationProperties.NO_CACHE_HEADERS);
        responseHeaders.putAll(headers);

        if(body != null) {

            responseHeaders.put(HttpHeader.CONTENT_TYPE.getName(), mimeType.getMimeValue());

            if(body instanceof final byte[] byteArrayBody) {

                exchange.sendResponseHeaders(status.getCode(), byteArrayBody.length);
                exchange.getResponseBody().write(byteArrayBody);
                exchange.getResponseBody().flush();
            }

            else if(body instanceof final BodyStreamer callback) {

                exchange.sendResponseHeaders(status.getCode(), HttpExchange.RSPBODY_CHUNKED);
                callback.stream();
                exchange.getResponseBody().flush();
            }
        }

        else {

            exchange.sendResponseHeaders(status.getCode(), HttpExchange.RSPBODY_EMPTY);
        }

        exchange.getResponseBody().close();
        exchange.setAttribute(ApplicationProperties.REQUEST_HANDLED_ATTRIBUTE, true);
    }

    ///..
    public static long ipV4ToLong(final byte[] address) {

        long value = 0;

        value = value | (address[3] & 0xFF);
        value = (value << 8) | (address[2] & 0xFF);
        value = (value << 8) | (address[1] & 0xFF);

        return (value << 8) | (address[0] & 0xFF);
    }

    ///..
    public static String formatSize(final long bytes) {

        if(bytes < 1024) return bytes + " B";

        final int length = DATA_SIZE_UNITS.length - 1;
        double value = bytes;
        int unit = -1;

        while(value >= 1024 && unit < length) {

            value /= 1024;
            unit++;
        }

        return String.format("%.1f %s", value, DATA_SIZE_UNITS[unit]).replace(".0 ", " ");
    }

    ///..
    public static String exchangeToString(final HttpExchange exchange) {

        return

            String.valueOf(exchange.getAttribute(ApplicationProperties.REQUEST_METHOD_ATTRIBUTE)) + ApplicationProperties.EXCHANGE_STRING_SEPARATOR +
            normalizedForObservability(String.valueOf(exchange.getRequestURI())) + ApplicationProperties.EXCHANGE_STRING_SEPARATOR +
            normalizedForObservability(String.valueOf(exchange.getRequestHeaders())) + ApplicationProperties.EXCHANGE_STRING_SEPARATOR +
            normalizedForObservability(String.valueOf(exchange.getResponseHeaders()))
        ;
    }

    ///..
    @SafeVarargs
    public static <K, V> Map<K, V> mutableMapOf(Entry<K, V>... entries) {

        if(entries == null) return new LinkedHashMap<>();
        final Map<K, V> map = LinkedHashMap.newLinkedHashMap(entries.length);

        for(final Entry<K, V> entry : entries) {

            if(entry != null) map.put(entry.getKey(), entry.getValue());
        }

        return map;
    }

    ///..
    public static <K, V> Map<K, V> mergeMaps(final Map<K, V> a, final Map<K, V> b) {

        if(a == null) return new HashMap<>();

        final Map<K, V> map = new HashMap<>(a);
        if(b != null) map.putAll(b);

        return map;
    }

    ///..
    public static String extractQueryParam(final String query, final String name) {

        if(query != null && !query.isEmpty() && name != null && !name.isEmpty()) {

            final List<String> queryStrings = fastSplit(query, '&');
            final int length = queryStrings.size();

            for(int i = 0; i < length; i++) {

                final String queryString = queryStrings.get(i);
                if(queryString.startsWith(name)) return queryString.substring(name.length() + 1);
            }
        }

        return null;
    }

    ///
}
