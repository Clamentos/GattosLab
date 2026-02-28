package io.github.clamentos.gattoslab.configuration;

///
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

///..
import io.github.clamentos.gattoslab.configuration.mappers.DynamicPropertyMapper;
import io.github.clamentos.gattoslab.persistence.DatabaseCollection;
import io.github.clamentos.gattoslab.persistence.EntityField;
import io.github.clamentos.gattoslab.persistence.MongoClientWrapper;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;

///..
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

///..
import lombok.extern.slf4j.Slf4j;

///..
import org.bson.Document;

///
@Slf4j

///
public final class DynamicProperties {

    ///
    private final MongoClientWrapper mongoClientWrapper;
    private final Map<DynamicPropertyType, Object> dynamicPropertyMap;

    ///
    public DynamicProperties(

        final ApplicationProperties applicationProperties,
        final BatchScheduler batchScheduler,
        final Map<DynamicPropertyType, DynamicPropertyMapper> mappers,
        final MongoClientWrapper mongoClientWrapper

    ) throws IllegalArgumentException {

        batchScheduler.schedule(() -> this.refresh(mappers), "DynamicProperties::refresh", applicationProperties.getDynamicPropertiesConfig().getSchedule());

        this.mongoClientWrapper = mongoClientWrapper;
        dynamicPropertyMap = new ConcurrentHashMap<>();
    }

    ///
    public <T> T get(final DynamicPropertyType type, final Class<T> clazz) throws ClassCastException {

        return clazz.cast(dynamicPropertyMap.get(type));
    }

    ///.
    private void refresh(final Map<DynamicPropertyType, DynamicPropertyMapper> mappers) {

        try {

            final MongoCollection<Document> collection = mongoClientWrapper.getCollection(DatabaseCollection.PROPERTIES);
            final MongoCursor<Document> properties = collection.find().iterator();

            while(properties.hasNext()) {

                final Document property = properties.next();
                final DynamicPropertyType type = DynamicPropertyType.valueOf(property.getString(EntityField.KEY.getField()));
                final Document rawValue = property.get(EntityField.VALUE.getField(), Document.class);

                final DynamicPropertyMapper mapper = mappers.get(type);
                final Object value = mapper != null ? mapper.map(rawValue) : rawValue;

                dynamicPropertyMap.put(type, value);
            }
        }

        catch(final IllegalArgumentException | MongoException exc) {

            log.error("Could not refresh the dynamic properties because", exc);
        }
    }

    ///
}
