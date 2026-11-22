package io.github.clamentos.gattoslab.server.components.connection;

///
import io.github.clamentos.gattoslab.datastructures.StreamWriter;
import io.github.clamentos.gattoslab.server.components.SocketReader;

///..
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

///..
import lombok.Getter;

///
@Getter

///
public final class ProxyConnection extends Connection {

    ///
    private final int index;
    private final SocketReader reader;
    private final StreamWriter writer;
    private final AtomicBoolean isBusy;

    ///
    public ProxyConnection(

        final int index,
        final Socket socket,
        final int inputBufferSize,
        final int outputBufferSize,
        final long maxTransfer,
        final int keepAliveDuration

    ) throws IOException {

        super(socket, null, keepAliveDuration);

        this.index = index;
        this.reader = new SocketReader(socket.getInputStream(), inputBufferSize, maxTransfer);
        this.writer = new StreamWriter(socket.getOutputStream(), outputBufferSize);
        this.isBusy = new AtomicBoolean();
    }

    ///
    public boolean aquire() {

        return this.isBusy.compareAndSet(false, true);
    }

    ///..
    public void release() {

        this.reader.resetAllowed();
        this.isBusy.set(false);
    }

    ///..
    @Override
    public void close() throws IOException {

        this.writer.close();
        super.close();
    }

    ///
}
