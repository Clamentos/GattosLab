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
public final class Acceptor {

    ///
    private final ServerSocket serverSocket;
    private final Thread acceptorThread;

    ///
}
