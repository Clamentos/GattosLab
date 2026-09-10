package io.github.clamentos.gattoslab.observability;

///
@FunctionalInterface

///
public interface RecordFilter {

    ///
    public boolean apply(final String recordLine, final long startTime, final long endTime);

    ///
}
