package io.github.clamentos.gattoslab.datastructures;

///
import io.github.clamentos.gattoslab.http.server.StreamWriter;

///..
import java.io.IOException;

///..
import lombok.EqualsAndHashCode;

///
@EqualsAndHashCode

///
public final class IpV4 implements IpAddress {

    ///
    private final int address;

    ///
    public IpV4(final byte[] address) {

        int value = 0;

        value =  value       | (address[0] & 0xFF);
        value = (value << 8) | (address[1] & 0xFF);
        value = (value << 8) | (address[2] & 0xFF);

        this.address = (value << 8) | (address[3] & 0xFF);
    }

    ///
    @Override
    public void stream(final StreamWriter writer) throws IOException {

        writer.write(Integer.toString((this.address & 0xFF000000) >> 24));
        writer.write('.');
        writer.write(Integer.toString((this.address & 0x00FF0000) >> 16));
        writer.write('.');
        writer.write(Integer.toString((this.address & 0x0000FF00) >> 8));
        writer.write('.');
        writer.write(Integer.toString(this.address & 0x000000FF));
    }

    ///..
    @Override
    public String toString() {

        final MutableString mutableString = new MutableString(15);

        mutableString.append(Integer.toString((this.address & 0xFF000000) >> 24));
        mutableString.append('.');
        mutableString.append(Integer.toString((this.address & 0x00FF0000) >> 16));
        mutableString.append('.');
        mutableString.append(Integer.toString((this.address & 0x0000FF00) >> 8));
        mutableString.append('.');
        mutableString.append(Integer.toString( this.address & 0x000000FF));

        return mutableString.toString();
    }

    ///
}
