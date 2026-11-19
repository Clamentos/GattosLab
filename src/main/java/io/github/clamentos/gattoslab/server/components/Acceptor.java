package io.github.clamentos.gattoslab.server.components;

///
import java.net.ServerSocket;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public sealed class Acceptor permits Tunnel {

    ///
    private final ServerSocket serverSocket;
    private final Thread acceptorThread;

    ///
}
