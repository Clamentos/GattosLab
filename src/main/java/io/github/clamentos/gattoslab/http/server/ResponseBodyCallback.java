package io.github.clamentos.gattoslab.http.server;

///
import java.io.IOException;

///
@FunctionalInterface

///
public interface ResponseBodyCallback {

    ///
    public void writeBody(final StreamWriter writer) throws IOException;

    ///
}
