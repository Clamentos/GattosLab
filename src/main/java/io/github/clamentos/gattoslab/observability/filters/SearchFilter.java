package io.github.clamentos.gattoslab.observability.filters;

import java.util.Set;

///
import org.bson.conversions.Bson;

///..
import org.jspecify.annotations.NonNull;

///
public interface SearchFilter {

    ///
    @NonNull Bson toBsonFilter();
    @NonNull Bson getSorting();
    @NonNull Set<String> getExcludedFields();

    ///
}
