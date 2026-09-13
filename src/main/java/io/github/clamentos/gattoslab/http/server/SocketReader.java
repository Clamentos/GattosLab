package io.github.clamentos.gattoslab.http.server;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;

///..
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

///
public final class SocketReader {

    ///
    private final ByteBuffer buffer;
    private final InputStream inputStream;

    private int maxAllowed;
    private int usableBytes;

    ///
    public SocketReader(final InputStream inputStream, final int size) {

        this.buffer = ByteBuffer.allocate(size);
        this.inputStream = inputStream;

        this.maxAllowed = ApplicationProperties.MAX_REQUEST_SIZE;
        this.usableBytes = 0;
    }

    ///
    public CharSequence readLine() throws IOException {

        final StringBuilder stringBuilder = new StringBuilder(80);

        while(true) {

            boolean crFound = false;
            if(this.usableBytes == 0 && !this.fill()) return null;

            while(this.usableBytes > 0) {

                final byte currentByte = this.buffer.get();

                this.usableBytes--;
                stringBuilder.append((char)currentByte);

                if(currentByte == '\r') {

                    crFound = true;
                }

                else if(currentByte == '\n' && crFound) {

                    stringBuilder.setLength(stringBuilder.length() - 2);
                    return stringBuilder;
                }
            }
        }
    }

    ///..
    public void resetAllowed() {

        this.maxAllowed = ApplicationProperties.MAX_REQUEST_SIZE;
    }

    ///.
    private boolean fill() throws IOException {

        if(this.maxAllowed <= 0) throw new IOException("Maximum request size exceeded");

        this.buffer.rewind();
        final int bytesRead = this.inputStream.read(this.buffer.array(), this.buffer.position(), this.buffer.remaining());

        if(bytesRead == -1) return false;

        this.maxAllowed -= bytesRead;
        this.usableBytes += bytesRead;

        return true;
    }

    ///
}
