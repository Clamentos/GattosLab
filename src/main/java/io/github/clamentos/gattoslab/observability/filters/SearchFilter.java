package io.github.clamentos.gattoslab.observability.filters;

///
import org.bson.conversions.Bson;

///..
import org.jspecify.annotations.NonNull;

///
@FunctionalInterface

///
public interface SearchFilter {

    ///
    @NonNull Bson toBsonFilter();

    ///
}
