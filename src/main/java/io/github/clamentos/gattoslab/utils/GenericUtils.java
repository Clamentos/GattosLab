package io.github.clamentos.gattoslab.utils;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.MutableString;
import io.github.clamentos.gattoslab.datastructures.StreamWriter;
import io.github.clamentos.gattoslab.server.http.exchange.components.HttpExchange;
import io.github.clamentos.gattoslab.server.http.exchange.components.HttpHeader;

///..
import java.io.IOException;
import java.lang.Thread.Builder.OfVirtual;
import java.nio.charset.StandardCharsets;
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
    public static final byte[] NEW_LINE_BYTES = "\r\n".getBytes(StandardCharsets.ISO_8859_1);

    ///..
    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.systemDefault();
    public static final OfVirtual OF_VIRTUAL = Thread.ofVirtual();

    ///..
    public static final byte[][] IPV4_LOOKUP_TABLE = new byte[256][];
    public static final byte[][] IPV6_LOOKUP_TABLE = new byte[65536][];
    public static final byte[][] CHUNKED_LOOKUP_TABLE = new byte[65536][];

    ///..
    public static final long MIN_ISO_DATE_TIMESTAMP = -62_167_222_196_000L;
    public static final long MAX_ISO_DATE_TIMESTAMP = 253_402_297_199_999L;

    ///..
    private static final String[] DATA_SIZE_UNITS = {"KiB", "MiB"};

    ///
    static {

        for(int i = 0; i < IPV4_LOOKUP_TABLE.length; i++) {

            IPV4_LOOKUP_TABLE[i] = Integer.toString(i).getBytes(StandardCharsets.ISO_8859_1);
        }

        for(int i = 0; i < IPV6_LOOKUP_TABLE.length; i++) {

            IPV6_LOOKUP_TABLE[i] = Integer.toHexString(i).getBytes(StandardCharsets.ISO_8859_1);
        }

        for(int i = 0; i < CHUNKED_LOOKUP_TABLE.length; i++) {

            CHUNKED_LOOKUP_TABLE[i] = (Integer.toHexString(i) + "\r\n").getBytes(StandardCharsets.ISO_8859_1);
        }
    }

    ///
    public static String composeFingerprint(final HttpExchange exchange) {

        return

            exchange.getClientRemoteAddress().toString() +
            ApplicationProperties.FINGERPRINT_SEPARATOR +
            exchange.getDomain() +
            ApplicationProperties.FINGERPRINT_SEPARATOR +
            normalizedForObservability(exchange.getRequestHeaders().get(HttpHeader.USER_AGENT))
        ;
    }

    ///..
    public static boolean silentSleep(final long millis) {

        return silentSleepInternal(millis, 0);
    }

    ///..
    public static boolean silentSleepNanos(final int nanos) {

        return silentSleepInternal(0, nanos);
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

        if(input == null || input.isEmpty()) return input;

        final int length = input.length();
        final MutableString mutableString = new MutableString(length);

        for(int i = 0; i < length; i++) {

            final char currentChar = input.charAt(i);

            if(currentChar == '\n' || currentChar == ApplicationProperties.FIELD_SEPARATOR) {

                mutableString.append(ApplicationProperties.ILLEGAL_CHAR_REPLACEMENT);
            }

            else {

                mutableString.append(currentChar);
            }
        }

        return mutableString;
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
    public static String headersToString(final Map<HttpHeader, String> headers) {

        final MutableString mutableString = new MutableString(128);

        for(final Entry<HttpHeader, String> header : headers.entrySet()) {

            mutableString.append(header.getKey().getResponseValue());
            mutableString.append(header.getValue());
            mutableString.append(NEW_LINE_BYTES);
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
    public static void longArrayToString(final long[] data, final StreamWriter writer) throws IOException {

        final int length = data.length - 1;
        writer.write('[');

        for(int i = 0; i < length; i++) {

            writer.write(Long.toString(data[i]));
            writer.write(',');
        }

        if(length >= 0) writer.write(Long.toString(data[length]));
        writer.write(']');
    }

    ///.
    private static boolean silentSleepInternal(final long millis, final int nanos) {

        try {

            Thread.sleep(millis, nanos);
            return true;
        }

        catch(final InterruptedException _) {

            Thread.currentThread().interrupt();
            return false;
        }
    }

    ///
}
