package io.github.clamentos.gattoslab.http.server;

///
import java.io.IOException;

///
@FunctionalInterface

///
public interface Filter {

    ///
    public boolean filter(final HttpExchange request) throws IOException;

    ///
}
