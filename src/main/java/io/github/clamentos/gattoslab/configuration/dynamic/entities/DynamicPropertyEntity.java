package io.github.clamentos.gattoslab.configuration.dynamic.entities;

///
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

///..
import io.github.clamentos.gattoslab.observability.filters.SearchFilter;
import io.github.clamentos.gattoslab.persistence.SearchableEntity;

///..
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

///
@AllArgsConstructor
@EqualsAndHashCode
@Getter

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)

@JsonSubTypes({

    @JsonSubTypes.Type(value = BlacklistDynamicProperty.class, name = "BLACKLIST")
})

///
public abstract class DynamicPropertyEntity implements SearchableEntity {

    ///
    private final DynamicPropertyType type;
    private final boolean enabled;

    ///
    @Override
    public boolean respectsFilter(final SearchFilter searchFilter) {

        return enabled;
    }

    ///
}
