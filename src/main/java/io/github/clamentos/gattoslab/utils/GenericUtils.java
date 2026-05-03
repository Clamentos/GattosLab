package io.github.clamentos.gattoslab.utils;

///
import io.github.clamentos.gattoslab.exceptions.CauseContainer;

///..
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

///..
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

///..
import org.bson.BsonReader;
import org.bson.BsonType;

///
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j

///
public final class GenericUtils {

    ///
    public static final String WRAPPED_EXCEPTION_MSG = "Wrapped exception";

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

    ///..
    public static <T> List<T> readList(final BsonReader reader, final Class<T> clazz) throws IllegalStateException {

        if(reader.getCurrentBsonType().equals(BsonType.NULL)) return null;
        final List<T> elements = new ArrayList<>();

        reader.readStartArray();

        while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

            if(clazz == String.class) elements.add(clazz.cast(reader.readString()));
            else if(clazz == Integer.class) elements.add(clazz.cast(reader.readInt32()));
            else if(clazz == Short.class) elements.add(clazz.cast((short)reader.readInt32()));

            else throw new IllegalStateException("Unsupported type '" + clazz.getSimpleName() + "'", new CauseContainer("GenericUtils.readList"));
        }

        reader.readEndArray();
        return elements;
    }

    ///..
    public static <T> Set<T> readSet(final BsonReader reader, final Class<T> clazz) throws IllegalStateException {

        if(reader.getCurrentBsonType().equals(BsonType.NULL)) return null;
        final Set<T> elements = new HashSet<>();

        reader.readStartArray();

        while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {

            if(clazz == String.class) elements.add(clazz.cast(reader.readString()));
            else if(clazz == Short.class) elements.add(clazz.cast((short)reader.readInt32()));

            else throw new IllegalStateException("Unsupported type '" + clazz.getSimpleName() + "'", new CauseContainer("GenericUtils.readSet"));
        }

        reader.readEndArray();
        return elements;
    }

    ///..
    public static String readString(final BsonReader reader) throws IllegalStateException {

        if(reader.getCurrentBsonType().equals(BsonType.NULL)) return null;
        return reader.readString();
    }

    ///..
    public static Boolean readBoolean(final BsonReader reader) throws IllegalStateException {

        if(reader.getCurrentBsonType().equals(BsonType.NULL)) return null;
        return reader.readBoolean();
    }

    ///
}
