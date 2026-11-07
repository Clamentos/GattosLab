package io.github.clamentos.gattoslab.datastructures.address;

///
import io.github.clamentos.gattoslab.datastructures.MutableString;
import io.github.clamentos.gattoslab.datastructures.StreamWriter;
import io.github.clamentos.gattoslab.utils.GenericUtils;

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

        writer.write(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.high & 0xFFFF_0000_0000_0000L) >>> 48]);
        writer.write(':');
        writer.write(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.high & 0x0000_FFFF_0000_0000L) >>> 32]);
        writer.write(':');
        writer.write(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.high & 0x0000_0000_FFFF_0000L) >>> 16]);
        writer.write(':');
        writer.write(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.high & 0x0000_0000_0000_FFFFL)]);
        writer.write(':');
        writer.write(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.low & 0xFFFF_0000_0000_0000L) >>> 48]);
        writer.write(':');
        writer.write(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.low & 0x0000_FFFF_0000_0000L) >>> 32]);
        writer.write(':');
        writer.write(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.low & 0x0000_0000_FFFF_0000L) >>> 16]);
        writer.write(':');
        writer.write(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.low & 0x0000_0000_0000_FFFFL)]);
    }

    ///..
    @Override
    public String toString() {

        final MutableString mutableString = new MutableString(15);

        mutableString.append(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.high & 0xFFFF_0000_0000_0000L) >>> 48]);
        mutableString.append(':');
        mutableString.append(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.high & 0x0000_FFFF_0000_0000L) >>> 32]);
        mutableString.append(':');
        mutableString.append(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.high & 0x0000_0000_FFFF_0000L) >>> 16]);
        mutableString.append(':');
        mutableString.append(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.high & 0x0000_0000_0000_FFFFL)]);
        mutableString.append(':');
        mutableString.append(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.low & 0xFFFF_0000_0000_0000L) >>> 48]);
        mutableString.append(':');
        mutableString.append(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.low & 0x0000_FFFF_0000_0000L) >>> 32]);
        mutableString.append(':');
        mutableString.append(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.low & 0x0000_0000_FFFF_0000L) >>> 16]);
        mutableString.append(':');
        mutableString.append(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.low & 0x0000_0000_0000_FFFFL)]);

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
