package io.github.clamentos.gattoslab.configuration.dynamic;

///
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;

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

///..
import org.bson.conversions.Bson;

///
@Slf4j

///
public final class DynamicProperties {

    ///
    private final Bson filterByEnabled;
    private final MongoClientWrapper mongoClientWrapper;

    ///..
    private final Map<DynamicPropertyType, DynamicPropertyEntity<?>> dynamicPropertyMap;

    ///
    public DynamicProperties(final ApplicationProperties applicationProperties, final BatchScheduler batchScheduler, final MongoClientWrapper mongoClientWrapper)
    throws IllegalArgumentException {

        batchScheduler.schedule(this::refresh, "DynamicProperties::refresh", applicationProperties.getDynamicPropertiesConfig().getSchedule());

        filterByEnabled = Filters.eq("enabled", true);
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

        final MongoCollection<DynamicPropertyEntity<?>> collection = mongoClientWrapper.getCollection(DatabaseCollection.PROPERTIES);

        try(final MongoCursor<DynamicPropertyEntity<?>> properties = collection.find(filterByEnabled).iterator()) {

            dynamicPropertyMap.clear();

            while(properties.hasNext()) {

                final DynamicPropertyEntity<?> property = properties.next();
                dynamicPropertyMap.put(property.getKey(), property);
            }
        }

        catch(final RuntimeException exc) {

            log.error("Could not refresh the dynamic properties because", exc);
        }
    }

    ///
}
