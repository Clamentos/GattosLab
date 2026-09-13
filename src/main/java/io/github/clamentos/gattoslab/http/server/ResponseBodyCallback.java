package io.github.clamentos.gattoslab.http.server;

///
import java.io.IOException;
import java.io.OutputStream;

///
@FunctionalInterface

///
public interface ResponseBodyCallback {

    ///
    public void writeBody(final OutputStream outputStream) throws IOException;

    ///
}
