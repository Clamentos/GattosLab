package io.github.clamentos.gattoslab.observability.database;

///
@FunctionalInterface

///
public interface RecordFilter {

    ///
    public boolean apply(final String line, final long startTime, final long endTime);

    ///
}
