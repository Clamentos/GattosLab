package io.github.clamentos.gattoslab.datastructures;

///
import lombok.EqualsAndHashCode;
import lombok.Getter;

///
@EqualsAndHashCode
@Getter

///
public final class HashCodedByteArray {

    ///
    private final byte[] data;

    ///
    public HashCodedByteArray(final byte[] data) {

        this.data = data;
    }

    ///
}
