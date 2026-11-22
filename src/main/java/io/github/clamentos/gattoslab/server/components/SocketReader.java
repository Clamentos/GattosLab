package io.github.clamentos.gattoslab.server.components;

///
import io.github.clamentos.gattoslab.datastructures.MutableString;
import io.github.clamentos.gattoslab.exceptions.RequestTooBigException;

///..
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

///
public final class SocketReader {

    ///
    private final byte[] buffer;
    private final InputStream inputStream;

    ///..
    private final long maximumTransferSize;
    private final String transferTooBigMessage;

    ///..
    private long maxAllowedBytes;
    private int usableBytes;
    private int position;

    ///
    public SocketReader(final InputStream inputStream, final int size, final long maximumTransferSize) {

        this.buffer = new byte[size];
        this.inputStream = inputStream;

        this.maximumTransferSize = maximumTransferSize;
        this.transferTooBigMessage = "Transfer too big, maximum is " + maximumTransferSize + " bytes";

        this.maxAllowedBytes = maximumTransferSize;
        this.usableBytes = 0;
        this.position = 0;
    }

    ///
    public CharSequence readLine() throws IOException {

        final MutableString mutableString = new MutableString(128);
        boolean crFound = false;

        while(true) {

            if(this.usableBytes == 0 && !this.fill()) return null;

            while(this.usableBytes > 0) {

                final byte currentByte = this.buffer[this.position++];
                this.usableBytes--;

                if(currentByte == '\r') {

                    crFound = true;
                }

                else if(currentByte == '\n' && crFound) {

                    mutableString.deleteLastCharUnsafe();
                    return mutableString;
                }

                mutableString.append(currentByte);
            }
        }
    }

    ///..
    public void transferTo(final OutputStream outputStream, final long amount) throws IOException {

        long remaining = amount - this.usableBytes;
        outputStream.write(this.buffer, this.position, this.usableBytes);

        this.position = this.buffer.length;
        this.usableBytes = 0;

        while(remaining > 0 && this.fill()) {

            outputStream.write(this.buffer, this.position, this.usableBytes);
            remaining -= this.usableBytes;

            this.position = this.buffer.length;
            this.usableBytes = 0;
        }

        outputStream.flush();
    }

    ///..
    public void resetAllowed() {

        this.maxAllowedBytes = this.maximumTransferSize;
    }

    ///.
    private boolean fill() throws IOException {

        if(this.maxAllowedBytes <= 0) throw new RequestTooBigException(this.transferTooBigMessage);

        final int bytesRead = this.inputStream.read(this.buffer, 0, this.buffer.length);
        if(bytesRead == -1) return false;

        this.position = 0;
        this.maxAllowedBytes -= bytesRead;
        this.usableBytes += bytesRead;

        return true;
    }

    ///
}
