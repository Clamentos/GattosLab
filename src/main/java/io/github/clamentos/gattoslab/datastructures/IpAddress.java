package io.github.clamentos.gattoslab.datastructures;

///
import io.github.clamentos.gattoslab.exchange.handling.components.Streamable;

///
public sealed interface IpAddress extends Streamable permits IpV4, IpV6 {

    ///
    public int hashCode();
    public boolean equals(final Object other);
    public String toString();

    ///
}
