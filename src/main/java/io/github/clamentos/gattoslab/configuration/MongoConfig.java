package io.github.clamentos.gattoslab.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

@Configuration
public class MongoConfig {

    @Bean
    public MongoClient mongoClient() {

        return MongoClients.create("mongodb://localhost:27017/GattosLabMongoDB");
    }
}
