package io.github.clamentos.gattoslab.server.components.connection;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.StreamWriter;
import io.github.clamentos.gattoslab.server.components.SocketReader;

///..
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

///..
import lombok.Getter;
import lombok.Setter;

///
public final class ProxyConnection extends Connection {

    ///
    @Getter
    private final int index;

    @Getter
    private final SocketReader proxyReader;
    
    @Getter
    private final StreamWriter proxyWriter;

    ///..
    private final AtomicBoolean isBusy;

    ///..
    @Getter @Setter
    private int keepAliveDuration;

    ///
    public ProxyConnection(final int index, final Socket socket) throws IOException {

        super(socket, null);

        this.index = index;
        this.proxyReader = new SocketReader(socket.getInputStream(), ApplicationProperties.HTTP_PROXY_INPUT_BUFFERS_SIZE);
        this.proxyWriter = new StreamWriter(socket.getOutputStream(), ApplicationProperties.HTTP_PROXY_OUTPUT_BUFFERS_SIZE);

        this.isBusy = new AtomicBoolean();
        this.keepAliveDuration = 0;
    }

    ///
    public boolean aquire() {

        return this.isBusy.compareAndSet(false, true);
    }

    ///..
    public void release() {

        this.isBusy.set(false);
    }

    ///
}
