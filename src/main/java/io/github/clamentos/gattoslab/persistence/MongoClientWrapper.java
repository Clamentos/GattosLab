package io.github.clamentos.gattoslab.persistence;

///
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

///.
import io.github.clamentos.gattoslab.utils.PropertyProvider;

///.
import java.util.EnumMap;
import java.util.Map;

///.
import lombok.Getter;

///.
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

///
@Component

///
public final class MongoClientWrapper {

    ///
    @Getter
    final MongoClient client;
    final Map<DatabaseCollection, MongoCollection<Document>> collections;

    ///
    @Autowired
    public MongoClientWrapper(final PropertyProvider propertyProvider) {

        final ConnectionString connectionString = new ConnectionString(propertyProvider.getProperty("app.database.connectionString", String.class));

        client = MongoClients.create(connectionString);
        collections = new EnumMap<>(DatabaseCollection.class);

        final MongoDatabase database = client.getDatabase(connectionString.getDatabase());

        for(final DatabaseCollection databaseCollection : DatabaseCollection.values()) {

            collections.put(databaseCollection, database.getCollection(databaseCollection.getValue()));
        }
    }

    ///
    public MongoCollection<Document> getCollection(final DatabaseCollection collection) {

        return collections.get(collection);
    }

    ///
}
