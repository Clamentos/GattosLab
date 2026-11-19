package io.github.clamentos.gattoslab.datastructures;

///
import java.util.Objects;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

///
@AllArgsConstructor
@Getter
@Setter

///
public class Pair<A, B> {

    ///
    private final A a;
    private B b;

    ///
    @Override
    public boolean equals(final Object other) {

        if(other == this) return true;

        if(other instanceof final Pair<?, ?> casted) {

            return Objects.equals(casted.a, this.a) && Objects.equals(casted.b, this.b);
        }

        return false;
    }

    ///..
    @Override
    public int hashCode() {

        return Objects.hashCode(a) ^ Objects.hashCode(b);
    }

    ///
}
