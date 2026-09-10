package io.github.clamentos.gattoslab.http;

///
import java.io.IOException;

///
@FunctionalInterface

///
public interface BodyStreamer {

    ///
    public void stream() throws IOException;

    ///
}
