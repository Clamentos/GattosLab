package io.github.clamentos.gattoslab.exchange.handling.components;

///
import io.github.clamentos.gattoslab.http.server.StreamWriter;

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
