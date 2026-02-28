package io.github.clamentos.gattoslab.utils;

///
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

///
public final class CompressingOutputStream extends GZIPOutputStream {

    ///
    public CompressingOutputStream(final OutputStream out) throws IOException {

        this(out, 0);
    }

    ///..
    public CompressingOutputStream(final OutputStream out, final int level) throws IOException {

        super(out);
        if(level > 0) super.def.setLevel(level);
    }

    ///
}
