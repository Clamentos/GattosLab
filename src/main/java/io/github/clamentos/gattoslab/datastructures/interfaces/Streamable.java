package io.github.clamentos.gattoslab.datastructures.interfaces;

import io.github.clamentos.gattoslab.datastructures.StreamWriter;

///..
import java.io.IOException;

///
@FunctionalInterface

///
public interface Streamable {

    ///
    public void stream(final StreamWriter writer) throws IOException;

    ///
}
