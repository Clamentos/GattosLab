package io.github.clamentos.gattoslab.datastructures;

///
import java.util.Arrays;

///
public final class HashCodedByteArray {

    ///
    private final byte[] data;

    ///
    public HashCodedByteArray(final byte[] data) {

        this.data = data;
    }

    ///
    @Override
    public boolean equals(final Object other) {

        if(other instanceof final HashCodedByteArray casted) {

            return Arrays.equals(this.data, casted.data);
        }

        return false;
    }

    ///..
    @Override
    public int hashCode() {

        return Arrays.hashCode(data);
    }

    ///
}
