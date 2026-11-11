package io.github.clamentos.gattoslab.configuration;

///
import java.net.InetSocketAddress;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public class ProxyConfiguration {

    ///
    private final InetSocketAddress listeningAddress;
    private final InetSocketAddress destinationAddress;

    ///
}
