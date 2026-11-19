package io.github.clamentos.gattoslab.server.components;

///
import io.github.clamentos.gattoslab.datastructures.Pair;

///..
import java.net.ServerSocket;

///..
import lombok.Getter;

///
@Getter

///
public final class Tunnel extends Acceptor {

    ///
    private final Pair<String, Integer> destinationAddress;

    ///
    public Tunnel(final ServerSocket serverSocket, final Thread acceptorThread, final Pair<String, Integer> destinationAddress) {

        super(serverSocket, acceptorThread);
        this.destinationAddress = destinationAddress;
    }

    ///
}
