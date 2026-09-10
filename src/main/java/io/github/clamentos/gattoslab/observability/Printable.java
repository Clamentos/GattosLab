package io.github.clamentos.gattoslab.observability;

///
import io.github.clamentos.gattoslab.datastructures.FastAsciiJoiner;

///
@FunctionalInterface

///
public interface Printable {

    ///
    public void appendBytes(final FastAsciiJoiner joiner);

    ///
}
