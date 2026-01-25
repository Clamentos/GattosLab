package io.github.clamentos.gattoslab.utils;

///
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

///.
import org.jspecify.annotations.NonNull;

///
public final class CompressingOutputStream extends GZIPOutputStream {

    ///
    public CompressingOutputStream(@NonNull final OutputStream out) throws IOException {

        this(out, 0);
    }

    ///..
    public CompressingOutputStream(@NonNull final OutputStream out, final int level) throws IOException {

        super(out);
        if(level > 0) super.def.setLevel(level);
    }

    ///
}
