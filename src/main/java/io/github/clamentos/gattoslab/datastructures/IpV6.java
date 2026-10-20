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
public final class IpV6 implements IpAddress {

    ///
    private final long low;
    private final long high;

    ///
    public IpV6(final byte[] address) {

        this.high = this.parse(address, 0);
        this.low = this.parse(address, 8);
    }

    ///
    @Override
    public void stream(final StreamWriter writer) throws IOException {

        writer.write(Long.toHexString((this.high & 0xFFFF000000000000L) >> 48));
        writer.write(':');
        writer.write(Long.toHexString((this.high & 0x0000FFFF00000000L) >> 32));
        writer.write(':');
        writer.write(Long.toHexString((this.high & 0x00000000FFFF0000L) >> 16));
        writer.write(':');
        writer.write(Long.toHexString(this.high & 0x000000000000FFFFL));
        writer.write(':');
        writer.write(Long.toHexString((this.low & 0xFFFF000000000000L) >> 48));
        writer.write(':');
        writer.write(Long.toHexString((this.low & 0x0000FFFF00000000L) >> 32));
        writer.write(':');
        writer.write(Long.toHexString((this.low & 0x00000000FFFF0000L) >> 16));
        writer.write(':');
        writer.write(Long.toHexString(this.low & 0x000000000000FFFFL));
    }

    ///..
    @Override
    public String toString() {

        final MutableString mutableString = new MutableString(15);

        mutableString.append(Long.toHexString((this.high & 0xFFFF000000000000L) >> 48));
        mutableString.append(':');
        mutableString.append(Long.toHexString((this.high & 0x0000FFFF00000000L) >> 32));
        mutableString.append(':');
        mutableString.append(Long.toHexString((this.high & 0x00000000FFFF0000L) >> 16));
        mutableString.append(':');
        mutableString.append(Long.toHexString(this.high & 0x000000000000FFFFL));
        mutableString.append(':');
        mutableString.append(Long.toHexString((this.low & 0xFFFF000000000000L) >> 48));
        mutableString.append(':');
        mutableString.append(Long.toHexString((this.low & 0x0000FFFF00000000L) >> 32));
        mutableString.append(':');
        mutableString.append(Long.toHexString((this.low & 0x00000000FFFF0000L) >> 16));
        mutableString.append(':');
        mutableString.append(Long.toHexString(this.low & 0x000000000000FFFFL));

        return mutableString.toString();
    }

    ///.
    private long parse(final byte[] address, final int offset) {

        long value = 0;

        for(int i = 0; i < Long.BYTES; i++) {

            value = (value << Long.BYTES) | (address[i + offset] & 0xFF);
        }

        return value;
    }

    ///
}
