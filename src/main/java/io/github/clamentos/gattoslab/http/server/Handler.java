package io.github.clamentos.gattoslab.http.server;

///
import java.io.IOException;

///
@FunctionalInterface 

///
public interface Handler {

    ///
    public void handle(final HttpExchange exchange) throws IOException;

    ///
}
