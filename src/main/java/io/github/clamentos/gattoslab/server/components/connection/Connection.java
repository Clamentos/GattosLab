package io.github.clamentos.gattoslab.server.components.connection;

///
import io.github.clamentos.gattoslab.datastructures.addresses.IpAddress;

///..
import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

///..
import lombok.Getter;

///
public sealed class Connection implements Closeable permits ProxyConnection {

    ///
    @Getter
    private final Socket socket;

    @Getter
    private final IpAddress clientRemoteAddress;

    private final AtomicInteger age;

    ///
    public Connection(final Socket socket, final IpAddress clientRemoteAddress, final int age) {

        this.socket = socket;
        this.clientRemoteAddress = clientRemoteAddress;
        this.age = new AtomicInteger(age);
    }

    ///
    public boolean isValid() {

        return

            !this.socket.isClosed() &&
            !this.socket.isInputShutdown() &&
            !this.socket.isOutputShutdown() &&
            this.age.get() > 0
        ;
    }

    ///..
    public int getAge() {

        return this.age.get();
    }

    ///..
    public void advanceAge() {

        this.age.decrementAndGet();
    }

    ///..
    @Override
    public void close() throws IOException {

        this.age.set(0);
        this.socket.close();
    }

    ///
}
