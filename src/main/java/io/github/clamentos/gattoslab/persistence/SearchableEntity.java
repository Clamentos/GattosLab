package io.github.clamentos.gattoslab.persistence;

///
import io.github.clamentos.gattoslab.observability.filters.SearchFilter;

///
@FunctionalInterface

///
public interface SearchableEntity {

    ///
    boolean respectsFilter(final SearchFilter searchFilter);

    ///
}
