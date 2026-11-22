package io.github.clamentos.gattoslab.configuration.components;

///
import io.github.clamentos.gattoslab.datastructures.Pair;

///..
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

///
@AllArgsConstructor
@EqualsAndHashCode(of = {"listeningAddress"})
@Getter

///
public class RawProxy {

    ///
    private final Pair<String, Integer> listeningAddress;
    private final Pair<String, Integer> destinationAddress;
    private final int keepAliveDuration;

    ///
}
