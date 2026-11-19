package io.github.clamentos.gattoslab.server.components.connection;

import io.github.clamentos.gattoslab.datastructures.addresses.IpAddress;

///
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

///..
import lombok.Getter;

///
@Getter

///
public sealed class Connection permits ProxyConnection {

    ///
    private final Socket socket;
    private final IpAddress clientRemoteAddress;
    private final long createdAt;
    private final AtomicBoolean swept;

    ///
    public Connection(final Socket socket, final IpAddress clientRemoteAddress) {

        this.socket = socket;
        this.clientRemoteAddress = clientRemoteAddress;
        this.createdAt = System.currentTimeMillis();
        this.swept = new AtomicBoolean();
    }

    ///
    public boolean isSwept() {

        return this.swept.get();
    }

    ///..
    public void sweep() {

        this.swept.set(true);
    }

    ///..
    public boolean isValid() {

        return !this.socket.isClosed() && !this.socket.isInputShutdown() && !this.socket.isOutputShutdown();
    }

    ///
}
