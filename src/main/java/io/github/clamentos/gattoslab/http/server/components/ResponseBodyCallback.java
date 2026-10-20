package io.github.clamentos.gattoslab.http.server.components;

///
import io.github.clamentos.gattoslab.datastructures.StreamWriter;

///..
import java.io.IOException;

///
@FunctionalInterface

///
public interface ResponseBodyCallback {

    ///
    public void writeBody(final StreamWriter writer) throws IOException;

    ///
}
