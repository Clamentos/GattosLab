package io.github.clamentos.gattoslab.datastructures.addresses;

///
import io.github.clamentos.gattoslab.datastructures.MutableString;
import io.github.clamentos.gattoslab.datastructures.StreamWriter;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.IOException;

///
public final class IpV6 implements IpAddress {

    ///
    private final long low;
    private final long high;

    ///
    public IpV6(final byte[] address) {

        this.high = this.parse(address, 0);
        this.low = this.parse(address, Long.BYTES);
    }

    ///
    @Override
    public void stream(final StreamWriter writer) throws IOException {

        writer.write(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.high & 0xFFFF_0000_0000_0000L) >>> 48]); writer.write(':');
        writer.write(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.high & 0x0000_FFFF_0000_0000L) >>> 32]); writer.write(':');
        writer.write(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.high & 0x0000_0000_FFFF_0000L) >>> 16]); writer.write(':');
        writer.write(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.high & 0x0000_0000_0000_FFFFL)]); writer.write(':');
        writer.write(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.low & 0xFFFF_0000_0000_0000L) >>> 48]); writer.write(':');
        writer.write(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.low & 0x0000_FFFF_0000_0000L) >>> 32]); writer.write(':');
        writer.write(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.low & 0x0000_0000_FFFF_0000L) >>> 16]); writer.write(':');
        writer.write(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.low & 0x0000_0000_0000_FFFFL)]);
    }

    ///..
    @Override
    public String toString() {

        final MutableString mutableString = new MutableString(39);

        mutableString.append(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.high & 0xFFFF_0000_0000_0000L) >>> 48]); mutableString.append(':');
        mutableString.append(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.high & 0x0000_FFFF_0000_0000L) >>> 32]); mutableString.append(':');
        mutableString.append(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.high & 0x0000_0000_FFFF_0000L) >>> 16]); mutableString.append(':');
        mutableString.append(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.high & 0x0000_0000_0000_FFFFL)]); mutableString.append(':');
        mutableString.append(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.low & 0xFFFF_0000_0000_0000L) >>> 48]); mutableString.append(':');
        mutableString.append(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.low & 0x0000_FFFF_0000_0000L) >>> 32]); mutableString.append(':');
        mutableString.append(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.low & 0x0000_0000_FFFF_0000L) >>> 16]); mutableString.append(':');
        mutableString.append(GenericUtils.IPV4_LOOKUP_TABLE[(int)(this.low & 0x0000_0000_0000_FFFFL)]);

        return mutableString.toString();
    }

    ///..
    @Override
    public boolean equals(final Object other) {

        if(other == this) return true;

        if(other instanceof final IpV6 casted) {

            return casted.low == this.low && casted.high == this.high;
        }

        return false;
    }

    ///..
    @Override
    public int hashCode() {

        int hash = 1;

        hash = (hash * 59) + (int)(this.low ^ (this.low >>> Integer.SIZE));
        hash = (hash * 59) + (int)(this.high ^ (this.high >>> Integer.SIZE));

        return hash;
    }

    ///.
    private long parse(final byte[] address, final int offset) {

        long value = 0;

        for(int i = 0; i < Long.BYTES; i++) {

            value = (value << Byte.SIZE) | (address[i + offset] & 0x00_00_00_FF);
        }

        return value;
    }

    ///
}
