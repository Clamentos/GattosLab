package io.github.clamentos.gattoslab.utils;

///
import lombok.Getter;
import lombok.Setter;

///.
import tools.jackson.databind.annotation.JsonSerialize;

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
