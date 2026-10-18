package io.github.clamentos.gattoslab.http.server;

///
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

///..
import lombok.Getter;

///
@Getter

///
public final class HttpConnection {

    ///
    private final Socket socket;
    private final long createdAt;
    private final AtomicBoolean swept;

    ///
    public HttpConnection(final Socket socket) {

        this.socket = socket;
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
