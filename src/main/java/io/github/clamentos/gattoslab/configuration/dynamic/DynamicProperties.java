package io.github.clamentos.gattoslab.configuration.dynamic;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.dynamic.entities.DynamicPropertyEntity;
import io.github.clamentos.gattoslab.configuration.dynamic.entities.DynamicPropertyType;
import io.github.clamentos.gattoslab.persistence.FileDatabase;
import io.github.clamentos.gattoslab.scheduling.BatchScheduler;

///..
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

///..
import lombok.extern.slf4j.Slf4j;

///
@Slf4j

///
public final class DynamicProperties {

    ///
    private final FileDatabase fileDatabase;

    ///..
    private final Map<DynamicPropertyType, DynamicPropertyEntity> dynamicPropertyMap;

    ///
    public DynamicProperties(final ApplicationProperties applicationProperties, final BatchScheduler batchScheduler, final FileDatabase fileDatabase)
    throws IllegalArgumentException {

        batchScheduler.schedule(this::refresh, "DynamicProperties::refresh", applicationProperties.getDynamicPropertiesRefreshSchedule());

        this.fileDatabase = fileDatabase;
        dynamicPropertyMap = new ConcurrentHashMap<>();
    }

    ///
    public DynamicPropertyEntity get(final DynamicPropertyType type) throws ClassCastException {

        return dynamicPropertyMap.get(type);
    }

    ///.
    private void refresh() {

        try {

            final List<DynamicPropertyEntity> properties = fileDatabase.fetchDynamicProperties();
            final int originalHashCode = dynamicPropertyMap.hashCode();

            dynamicPropertyMap.clear();
            for(final DynamicPropertyEntity property : properties) dynamicPropertyMap.put(property.getType(), property);

            if(dynamicPropertyMap.hashCode() != originalHashCode) log.info("Dynamic properties have changed");
        }

        catch(final IOException | RuntimeException exc) {

            log.error("Could not refresh the dynamic properties because", exc);
        }
    }

    ///
}
