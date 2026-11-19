package io.github.clamentos.gattoslab.server.http.exchange.components.body;

///
import io.github.clamentos.gattoslab.datastructures.StreamWriter;

///..
import java.io.IOException;

///
@FunctionalInterface

///
public interface ChunkedResponseBodyCallback {

    ///
    public void writeBody(final StreamWriter writer) throws IOException;

    ///
}
