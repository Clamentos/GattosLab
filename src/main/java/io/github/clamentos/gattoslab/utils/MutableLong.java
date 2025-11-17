package io.github.clamentos.gattoslab.utils;

///
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

///.
import lombok.Getter;
import lombok.Setter;

///
@JsonSerialize(using = MutableLongSerializer.class)
@Getter
@Setter

///
public final class MutableLong {

    ///
    private long value;

    ///
    public void increment(final long amount) {

        value += amount;
    }

    ///
}
