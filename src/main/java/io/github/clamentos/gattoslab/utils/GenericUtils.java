package io.github.clamentos.gattoslab.utils;

///
import java.util.ArrayList;
import java.util.List;

///.
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

    ///..
    public static <T> List<T> initList(final int size, final T value) {

        final List<T> list = new ArrayList<>(size);
        for(int i = 0; i < size; i++) list.add(value);

        return list;
    }

    ///
}
