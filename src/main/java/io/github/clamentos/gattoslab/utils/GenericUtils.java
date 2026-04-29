package io.github.clamentos.gattoslab.utils;

///
import java.net.InetAddress;

///..
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

///
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j

///
public final class GenericUtils {

    ///
    public static String composeFingerprint(final InetAddress ip, final String userAgent) {

        return ip.getHostAddress() + " >> " + userAgent;
    }

    ///..
    public static boolean silentSleep(final long amount) {

        try {

            Thread.sleep(amount);
            return false;
        }

        catch(final InterruptedException _) {

            Thread.currentThread().interrupt();
            Thread.interrupted();

            return true;
        }
    }

    ///
}
