package io.github.clamentos.gattoslab.utils;

///
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

///.
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

///
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j

///
public final class GenericUtils {

    ///
    public static @NonNull String composeFingerprint(@Nullable final String ip, @Nullable final String userAgent) {

        return ip + " " + userAgent;
    }

    ///..
    public static void sleep(long amount) {

        try {

            Thread.sleep(amount);
        }

        catch(final InterruptedException exc) {

            Thread.currentThread().interrupt();
            log.warn("Interrupted while sleeping", exc);
        }
    }

    ///..
    public static <T> T getOrDefault(final T value, final T defaultValue) {

        return value != null ? value : defaultValue;
    }

    ///
}
