package io.github.clamentos.gattoslab.configuration.dynamic;

///
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.persistence.MongoClientWrapper;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;

///..
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

///..
import lombok.extern.slf4j.Slf4j;

///
@Slf4j

///
public final class DynamicProperties {

    ///
    private final MongoClientWrapper mongoClientWrapper;
    private final Map<DynamicPropertyType, DynamicPropertyEntity<?>> dynamicPropertyMap;

    ///
    public DynamicProperties(final ApplicationProperties applicationProperties, final BatchScheduler batchScheduler, final MongoClientWrapper mongoClientWrapper)
    throws IllegalArgumentException {

        batchScheduler.schedule(this::refresh, "DynamicProperties::refresh", applicationProperties.getDynamicPropertiesConfig().getSchedule());

        this.mongoClientWrapper = mongoClientWrapper;
        dynamicPropertyMap = new ConcurrentHashMap<>();
    }

    ///
    @SuppressWarnings("unchecked")
    public <T> DynamicPropertyEntity<T> get(final DynamicPropertyType type) throws ClassCastException {

        return (DynamicPropertyEntity<T>) dynamicPropertyMap.get(type);
    }

    ///.
    private void refresh() {

        try {

            final MongoCollection<DynamicPropertyEntity<?>> collection = mongoClientWrapper.getCollection(DatabaseCollection.PROPERTIES);
            final MongoCursor<DynamicPropertyEntity<?>> properties = collection.find().iterator();

            while(properties.hasNext()) {

                final DynamicPropertyEntity<?> entity = properties.next();
                dynamicPropertyMap.put(entity.getKey(), entity);
            }
        }

        catch(final RuntimeException exc) {

            log.error("Could not refresh the dynamic properties because", exc);
        }
    }

    ///
}
