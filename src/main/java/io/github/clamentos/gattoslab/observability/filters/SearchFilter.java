package io.github.clamentos.gattoslab.observability.filters;

///
import org.bson.conversions.Bson;

///
@FunctionalInterface

///
public interface SearchFilter {

    ///
    Bson toBsonFilter();

    ///
}
