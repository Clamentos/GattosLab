package io.github.clamentos.gattoslab.persistence;

///
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.connection.ClusterConnectionMode;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.dynamic.mappers.DynamicPropertyMapper;
import io.github.clamentos.gattoslab.configuration.pojos.DatabaseConfig;
import io.github.clamentos.gattoslab.observability.logging.mappers.LogEntityMapper;
import io.github.clamentos.gattoslab.observability.metrics.mappers.PathInvocationAggregationEntityMapper;
import io.github.clamentos.gattoslab.observability.metrics.mappers.RequestMetricsAggregateEntityMapper;
import io.github.clamentos.gattoslab.observability.metrics.mappers.RequestMetricsEntityMapper;
import io.github.clamentos.gattoslab.observability.metrics.mappers.SystemMetricsEntityMapper;
import io.github.clamentos.gattoslab.observability.metrics.mappers.UserAgentAggregationEntityMapper;

///..
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

///..
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

///..
import org.bson.codecs.configuration.CodecRegistries;

///
@Slf4j

///
public final class MongoClientWrapper {

    ///
    @Getter
    final MongoClient client;

    final Map<DatabaseCollection, MongoCollection<?>> collections;

    ///
    public MongoClientWrapper(final ApplicationProperties applicationProperties) throws IllegalArgumentException, MongoException {

        log.info("Connecting to database...");

        final DatabaseConfig databaseConfig = applicationProperties.getDatabaseConfig();
        final ConnectionString connectionString = new ConnectionString(databaseConfig.getConnectionString());

        final MongoClientSettings config = MongoClientSettings.builder()

            .applyConnectionString(connectionString)
            .applyToConnectionPoolSettings(pool -> {

                pool.minSize(databaseConfig.getMinPoolSize());
                pool.maxSize(databaseConfig.getMaxPoolSize());
                pool.maintenanceFrequency(databaseConfig.getMaintenanceFrequency(), TimeUnit.SECONDS);
                pool.maxConnectionIdleTime(databaseConfig.getMaxConnectionIdleTime(), TimeUnit.SECONDS);
            })
            .applyToSocketSettings(socket -> {

                socket.connectTimeout(databaseConfig.getConnectTimeout(), TimeUnit.SECONDS);
                socket.readTimeout(databaseConfig.getReadTimeout(), TimeUnit.SECONDS);
            })
            .applyToClusterSettings(cluster -> cluster.mode(ClusterConnectionMode.SINGLE))
            .readConcern(ReadConcern.LOCAL)
            .readPreference(ReadPreference.nearest())
            .writeConcern(WriteConcern.JOURNALED)
            .codecRegistry(

                CodecRegistries.fromRegistries(

                    CodecRegistries.fromCodecs(new SystemMetricsEntityMapper()),
                    CodecRegistries.fromCodecs(new RequestMetricsEntityMapper()),
                    CodecRegistries.fromCodecs(new LogEntityMapper()),
                    CodecRegistries.fromCodecs(new DynamicPropertyMapper()),
                    CodecRegistries.fromCodecs(new PathInvocationAggregationEntityMapper()),
                    CodecRegistries.fromCodecs(new UserAgentAggregationEntityMapper()),
                    CodecRegistries.fromCodecs(new RequestMetricsAggregateEntityMapper()),
                    MongoClientSettings.getDefaultCodecRegistry()
                )
            )
            .build()
        ;

        client = MongoClients.create(config);
        collections = new EnumMap<>(DatabaseCollection.class);

        final MongoDatabase database = client.getDatabase(connectionString.getDatabase());

        for(final DatabaseCollection databaseCollection : DatabaseCollection.values()) {

            collections.put(databaseCollection, database.getCollection(databaseCollection.getValue(), databaseCollection.getEntityClass()));
        }

        log.info("Database connection complete");
    }

    ///
    @SuppressWarnings("unchecked")
    public <T> MongoCollection<T> getCollection(final DatabaseCollection collection) {

        return (MongoCollection<T>)collections.get(collection);
    }

    ///..
    public <E> void insertAll(final List<E> documents, final DatabaseCollection collection) throws MongoException {

        if(!documents.isEmpty()) this.getCollection(collection).insertMany(documents);
    }

    ///
}
