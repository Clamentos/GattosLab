package io.github.clamentos.gattoslab.datastructures;

///
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

///
@AllArgsConstructor
@EqualsAndHashCode
@Getter

///
public class Pair<A, B> {

    ///
    private final A a;
    private final B b;

    ///
}
