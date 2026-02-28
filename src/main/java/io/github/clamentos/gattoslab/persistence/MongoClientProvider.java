package io.github.clamentos.gattoslab.persistence;

///
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

///
@NoArgsConstructor(access = AccessLevel.PRIVATE)

///
public final class MongoClientProvider {

    ///
    private static MongoClientWrapper mongoClientWrapper;

    ///
    public static MongoClientWrapper getWrapper() {

        return mongoClientWrapper;
    }

    ///..
    public static void setWrapper(final MongoClientWrapper client) {

        mongoClientWrapper = client;
    }

    ///
}
