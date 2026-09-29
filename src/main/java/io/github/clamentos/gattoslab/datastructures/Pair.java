package io.github.clamentos.gattoslab.datastructures;

///
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

///
@AllArgsConstructor
@Getter
@EqualsAndHashCode

///
public class Pair<A, B> {

    ///
    private final A a;
    private final B b;

    ///
}
