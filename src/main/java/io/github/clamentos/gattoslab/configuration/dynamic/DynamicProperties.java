package io.github.clamentos.gattoslab.configuration.dynamic;

///
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.persistence.EntityField;
import io.github.clamentos.gattoslab.persistence.MongoClientWrapper;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;
import io.github.clamentos.gattoslab.utils.Hashable;

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
    private final Map<DynamicPropertyType, DynamicPropertyEntity<? extends Hashable>> dynamicPropertyMap;

    ///
    public DynamicProperties(final ApplicationProperties applicationProperties, final BatchScheduler batchScheduler, final MongoClientWrapper mongoClientWrapper)
    throws IllegalArgumentException {

        batchScheduler.schedule(this::refresh, "DynamicProperties::refresh", applicationProperties.getDynamicPropertiesConfig().getSchedule());

        filterByEnabled = Filters.eq(EntityField.ENABLED, true);
        this.mongoClientWrapper = mongoClientWrapper;
        dynamicPropertyMap = new ConcurrentHashMap<>();
    }

    ///
    @SuppressWarnings("unchecked")
    public <T extends Hashable> DynamicPropertyEntity<T> get(final DynamicPropertyType type) throws ClassCastException {

        return (DynamicPropertyEntity<T>) dynamicPropertyMap.get(type);
    }

    ///.
    private void refresh() {

        final MongoCollection<DynamicPropertyEntity<? extends Hashable>> collection = mongoClientWrapper.getCollection(DatabaseCollection.PROPERTIES);

        try(final MongoCursor<DynamicPropertyEntity<? extends Hashable>> properties = collection.find(filterByEnabled).iterator()) {

            final int originalHashCode = dynamicPropertyMap.hashCode();
            dynamicPropertyMap.clear();

            while(properties.hasNext()) {

                final DynamicPropertyEntity<? extends Hashable> property = properties.next();
                dynamicPropertyMap.put(property.getKey(), property);
            }

            if(dynamicPropertyMap.hashCode() != originalHashCode) log.info("Dynamic property changes applied");
        }

        catch(final RuntimeException exc) {

            log.error("Could not refresh the dynamic properties because", exc);
        }
    }

    ///
}
