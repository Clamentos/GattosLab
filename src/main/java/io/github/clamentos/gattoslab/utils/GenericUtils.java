package io.github.clamentos.gattoslab.utils;

///
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

///
@NoArgsConstructor(access = AccessLevel.PRIVATE)

///
public final class GenericUtils {

    ///
    public static String composeFingerprint(final String ip, final String userAgent) {

        return ip + " " + userAgent;
    }

    ///
}
