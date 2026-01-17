package io.github.clamentos.gattoslab.persistence;

///
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

///.
import io.github.clamentos.gattoslab.configuration.PropertyProvider;

///.
import java.util.EnumMap;
import java.util.Map;

///.
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

///.
import org.bson.Document;

///..
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

///
@Component
@Slf4j

///
public final class MongoClientWrapper {

    ///
    @Getter
    final MongoClient client;

    final Map<DatabaseCollection, MongoCollection<Document>> collections;

    ///
    @Autowired
    public MongoClientWrapper(final PropertyProvider propertyProvider) {

        try {

            final ConnectionString connectionString = new ConnectionString(propertyProvider.getProperty("app.database.connectionString", String.class));

            client = MongoClients.create(connectionString);
            collections = new EnumMap<>(DatabaseCollection.class);

            final MongoDatabase database = client.getDatabase(connectionString.getDatabase());

            for(final DatabaseCollection databaseCollection : DatabaseCollection.values()) {

                collections.put(databaseCollection, database.getCollection(databaseCollection.getValue()));
            }
        }

        catch(final Exception exc) {

            log.error("Could not connect to the database because", exc);
            throw exc;
        }
    }

    ///
    public MongoCollection<Document> getCollection(final DatabaseCollection collection) {

        return collections.get(collection);
    }

    ///
}
