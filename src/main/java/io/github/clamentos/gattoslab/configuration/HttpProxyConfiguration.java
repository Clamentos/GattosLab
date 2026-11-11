package io.github.clamentos.gattoslab.configuration;

///
import java.net.InetSocketAddress;

///..
import lombok.Getter;

///
@Getter

///
public final class HttpProxyConfiguration {

    ///
    private final String domain;
    private final InetSocketAddress destinationAddress;
    private final boolean authenticated;

    ///
    public HttpProxyConfiguration(final String domain, final InetSocketAddress destinationAddress, final boolean authenticated) {

        this.domain = domain;
        this.destinationAddress = destinationAddress;
        this.authenticated = authenticated;
    }

    ///
}
