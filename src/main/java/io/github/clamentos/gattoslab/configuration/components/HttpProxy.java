package io.github.clamentos.gattoslab.configuration.components;

///
import io.github.clamentos.gattoslab.datastructures.Pair;

///..
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

///
@AllArgsConstructor
@Getter
@EqualsAndHashCode(of = {"domain"})

///
public final class HttpProxy {

    ///
    private final String domain;
    private final Pair<String, Integer> destinationAddress;
    private final boolean authenticated;

    ///
}
