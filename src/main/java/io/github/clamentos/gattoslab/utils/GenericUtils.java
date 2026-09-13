package io.github.clamentos.gattoslab.utils;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.server.HttpExchange;

///..
import java.lang.Thread.Builder.OfVirtual;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

///..
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

///
@NoArgsConstructor(access = AccessLevel.PRIVATE)

///
public final class GenericUtils {

    ///
    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();
    public static final OfVirtual OF_VIRTUAL = Thread.ofVirtual();

    ///..
    private static final String[] DATA_SIZE_UNITS = {"KiB", "MiB", "GiB", "TiB", "PiB", "EiB"};

    ///.
    public static String composeFingerprint(final HttpExchange exchange) {

        final StringJoiner joiner = new StringJoiner(ApplicationProperties.FINGERPRINT_SEPARATOR);
        final byte[] address = exchange.getRemoteAddress();

        joiner.add(composeAddressString(address));
        joiner.add(normalizedForObservability(exchange.getRequestHeaders().get(HttpHeader.USER_AGENT)));

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

        return OF_VIRTUAL.name(name).start(task);
    }

    ///..
    public static Thread createVirtualThread(final String name, final Runnable task) {

        return OF_VIRTUAL.name(name).unstarted(task);
    }

    ///..
    public static List<String> fastSplit(final CharSequence input, final char delimiter) {

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

            String.valueOf(exchange.getMethod()) + ApplicationProperties.EXCHANGE_STRING_SEPARATOR +
            normalizedForObservability(String.valueOf(exchange.getUri())) + ApplicationProperties.EXCHANGE_STRING_SEPARATOR +
            normalizedForObservability(String.valueOf(exchange.getRequestHeaders())) + ApplicationProperties.EXCHANGE_STRING_SEPARATOR +
            normalizedForObservability(String.valueOf(exchange.getResponseHeaders()))
        ;
    }

    ///..
    @SafeVarargs
    public static <V> Map<HttpHeader, V> headers(final Entry<HttpHeader, V>... entries) {

        final Map<HttpHeader, V> map = new EnumMap<>(HttpHeader.class);

        if(entries != null) {

            for(final Entry<HttpHeader, V> entry : entries) {

                if(entry != null) map.put(entry.getKey(), entry.getValue());
            }
        }

        return map;
    }

    ///..
    public static String extractQueryParam(final String uri, final String name) {

        if(uri == null || uri.isEmpty() || name == null || name.isEmpty()) return null;

        final List<String> pathSplits = fastSplit(uri, '?');
        if(pathSplits.size() != 2) return null;

        final List<String> queryStrings = fastSplit(pathSplits.get(1), '&');
        if(queryStrings == null) return null;

        final int length = queryStrings.size();

        for(int i = 0; i < length; i++) {

            final String queryString = queryStrings.get(i);
            if(queryString.startsWith(name)) return queryString.substring(name.length() + 1);
        }

        return null;
    }

    ///..
    public static String fastToLower(final String value, final StringBuilder buffer) {

        final int length = value.length();

        for(int i = 0; i < length; i++) {

            buffer.append(Character.toLowerCase(value.charAt(i)));
        }

        return buffer.toString();
    }

    ///..
    public static String composeMessageForSquash(final String prefix, final HttpExchange exchange) {

        return prefix + GenericUtils.composeFingerprint(exchange) + " " + ApplicationProperties.LOG_SQUASH_COUNTS_CHAR + " times";
    }

    ///..
    public static String concatenateAsCsv(final Collection<?> values) {

        return values.stream().map(Objects::toString).collect(Collectors.joining(", "));
    }

    ///.
    private static CharSequence composeAddressString(final byte[] address) {

        if(address == null) return "null";
        final StringBuilder builder = new StringBuilder(40);

        for(final byte section : address) {

            builder.append(Byte.toString(section));
        }

        return builder;
    }

    ///
}
