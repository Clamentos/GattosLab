package io.github.clamentos.gattoslab.utils;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.MutableString;
import io.github.clamentos.gattoslab.http.HttpHeaderName;
import io.github.clamentos.gattoslab.http.server.HttpExchange;

///..
import java.lang.Thread.Builder.OfVirtual;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
    private static final String[] DATA_SIZE_UNITS = {"KiB", "MiB", "GiB", "TiB"};

    ///.
    public static String composeFingerprint(final HttpExchange exchange) {

        return

            composeAddressString(exchange.getRemoteAddress()) +
            ApplicationProperties.FINGERPRINT_SEPARATOR +
            normalizedForObservability(exchange.getRequestHeaders().get(HttpHeaderName.USER_AGENT))
        ;
    }

    ///..
    public static void silentSleep(final long millis) {

        silentSleepInternal(millis, 0);
    }

    ///..
    public static void silentSleepNanos(final int nanos) {

        silentSleepInternal(0, nanos);
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

        if(input == null || input.isEmpty()) return List.of();

        final int length = input.length();
        final List<String> splits = new ArrayList<>(5);
        final MutableString mutableString = new MutableString(length);

        for(int i = 0; i < length; i++) {

            final char currentChar = input.charAt(i);

            if(currentChar != delimiter) {

                mutableString.append(currentChar);
            }

            else {

                splits.add(mutableString.toString());
                mutableString.clear();
            }
        }

        splits.add(mutableString.toString());
        return splits;
    }

    ///..
    public static CharSequence normalizedForObservability(final String input) {

        if(input == null) return null;
        if(input.isEmpty()) return "";

        final int length = input.length();
        final MutableString stringBuilder = new MutableString(length);

        for(int i = 0; i < length; i++) {

            final char currentChar = input.charAt(i);

            switch(currentChar) {

                case '\n': stringBuilder.append(ApplicationProperties.NEWLINE_REPLACEMENT); break;
                case ApplicationProperties.FIELD_SEPARATOR: stringBuilder.append(ApplicationProperties.FIELD_SEPARATOR_REPLACEMENT); break;

                default: stringBuilder.append(currentChar); break;
            }
        }

        return stringBuilder;
    }

    ///..
    public static long ipV4ToLong(final byte[] address) {

        long value = 0;

        value = value | (address[0] & 0xFF);
        value = (value << 8) | (address[1] & 0xFF);
        value = (value << 8) | (address[2] & 0xFF);

        return (value << 8) | (address[3] & 0xFF);
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

        return String.format(Locale.US, "%.1f %s", value, DATA_SIZE_UNITS[unit]).replace(".0 ", " ");
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
    public static String composeMessageForSquash(final String prefix, final HttpExchange exchange) {

        return

            prefix +
            " " +
            GenericUtils.composeFingerprint(exchange) +
            ApplicationProperties.EXCHANGE_STRING_SEPARATOR +
            ApplicationProperties.LOG_SQUASH_COUNTS_CHAR +
            " times"
        ;
    }

    ///..
    public static String composeMessageForSquash(final String message) {

        return message + " " + ApplicationProperties.LOG_SQUASH_COUNTS_CHAR + " times";
    }

    ///..
    public static String concatenateAsCsv(final Collection<?> values) {

        return values.stream().map(Objects::toString).collect(Collectors.joining(", "));
    }

    ///.
    private static void silentSleepInternal(final long millis, final int nanos) {

        try {

            Thread.sleep(millis, nanos);
        }

        catch(final InterruptedException _) {

            Thread.currentThread().interrupt();
        }
    }

    ///..
    private static CharSequence composeAddressString(final byte[] address) {

        if(address == null) return null;
        final MutableString mutableString = new MutableString(40);

        for(final byte section : address) {

            mutableString.append(Byte.toString(section));
            mutableString.append(address.length == 4 ? '.' : ':');
        }

        mutableString.deleteLastChars(1);
        return mutableString;
    }

    ///
}
