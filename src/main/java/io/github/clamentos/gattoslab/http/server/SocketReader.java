package io.github.clamentos.gattoslab.http.server;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.MutableString;
import io.github.clamentos.gattoslab.exceptions.RequestTooBigException;

///..
import java.io.IOException;
import java.io.InputStream;

///
public final class SocketReader {

    ///
    private static final String REQUEST_TOO_BIG_MESSAGE = "Request too big, maximum is " + ApplicationProperties.MAX_REQUEST_SIZE + " bytes";

    ///.
    private final byte[] buffer;
    private final InputStream inputStream;

    ///..
    private int maxAllowedBytes;
    private int usableBytes;
    private int position;

    ///
    public SocketReader(final InputStream inputStream, final int size) {

        this.buffer = new byte[size];
        this.inputStream = inputStream;

        this.maxAllowedBytes = ApplicationProperties.MAX_REQUEST_SIZE;
        this.usableBytes = 0;
        this.position = 0;
    }

    ///
    public CharSequence readLine() throws IOException {

        final MutableString mutableString = new MutableString(80);
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

                    mutableString.deleteLastChars(1);
                    return mutableString;
                }

                mutableString.append(currentByte);
            }
        }
    }

    ///..
    public void resetAllowed() {

        this.maxAllowedBytes = ApplicationProperties.MAX_REQUEST_SIZE;
    }

    ///.
    private boolean fill() throws IOException {

        if(this.maxAllowedBytes <= 0) throw new RequestTooBigException(REQUEST_TOO_BIG_MESSAGE);

        final int bytesRead = this.inputStream.read(this.buffer, 0, this.buffer.length);
        if(bytesRead == -1) return false;

        this.position = 0;
        this.maxAllowedBytes -= bytesRead;
        this.usableBytes += bytesRead;

        return true;
    }

    ///
}
