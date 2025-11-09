package io.github.clamentos.gattoslab.configuration;

///
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

///.
import io.github.clamentos.gattoslab.utils.PropertyProvider;

///.
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

///
@Configuration

///
public class MongoConfiguration {

    ///
    @Bean
    public MongoClient mongoClient(final PropertyProvider propertyProvider) {

        return MongoClients.create(propertyProvider.getProperty("app.database.connectionString", String.class));
    }

    ///
}
