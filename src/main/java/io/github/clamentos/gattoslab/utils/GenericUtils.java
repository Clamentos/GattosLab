package io.github.clamentos.gattoslab.utils;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.MutableString;
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.server.HttpExchange;
import io.github.clamentos.gattoslab.http.server.StreamWriter;

///..
import java.io.IOException;
import java.lang.Thread.Builder.OfVirtual;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
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

            composeAddressString(exchange.getRemoteIpAddress()) +
            ApplicationProperties.FINGERPRINT_SEPARATOR +
            normalizedForObservability(exchange.getRequestHeaders().get(HttpHeader.USER_AGENT))
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

            prefix + " " +
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

    ///..
    public static String headerToString(final Map<HttpHeader, String> headers) {

        final MutableString mutableString = new MutableString(128);

        for(final Entry<HttpHeader, String> header : headers.entrySet()) {

            mutableString.append(new String(header.getKey().getValueForResponse()));
            mutableString.append(header.getValue());
            mutableString.append("\r\n");
        }

        return mutableString.toString();
    }

    ///..
    public static boolean isExceptionNotable(final Throwable exc) {

        final String msg = exc.getMessage();

        return

            !(exc instanceof IOException) ||
            (msg != null && !msg.contains("closed") && !msg.contains("reset") && !msg.contains("interrupt") && !msg.contains("pipe"))
        ;
    }

    ///..
    public static CharSequence composeAddressString(final byte[] address) {

        if(address == null) return null;
        final MutableString mutableString = new MutableString(40);

        for(final byte section : address) {

            mutableString.append(Short.toString((short)(section & 0x00FF)));
            mutableString.append(address.length == 4 ? '.' : ':');
        }

        mutableString.deleteLastChars(1);
        return mutableString;
    }

    ///..
    public static void longToString(final long number, final StreamWriter writer, final byte[] buffer) throws IOException {

        if(number == 0) {

            writer.write('0');
            return;
        }

        long value = number;
        int idx = 0;

        while(value != 0) {

            final long quotient = value / 10;

            buffer[idx++] = (byte) -(value - quotient * 10);
            value = quotient;
        }

        if(number < 0) writer.write('-');

        while(idx > 0) {

            writer.write((byte)(buffer[--idx] + '0'));
        }
    }

    ///..
    public static void longArrayToString(final long[] data, final StreamWriter writer) throws IOException {

        final byte[] buffer = new byte[20];
        final int length = data.length - 1;

        writer.write('[');

        for(int i = 0; i < length; i++) {

            longToString(data[i], writer, buffer);
            writer.write(',');
        }

        if(length >= 0) longToString(length, writer, buffer);
        writer.write(']');
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

    ///
}
