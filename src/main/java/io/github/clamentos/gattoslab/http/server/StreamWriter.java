package io.github.clamentos.gattoslab.http.server;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;

///..
import java.io.IOException;
import java.io.OutputStream;

///
public final class StreamWriter extends OutputStream {

    ///
    private final byte[] buffer;
    private final OutputStream outputStream;

    ///..
    private int index;
    private boolean chunkedMode;

    ///
    public StreamWriter(final OutputStream outputStream, final int bufferSize) {

        this.buffer = new byte[bufferSize];
        this.outputStream = outputStream;

        this.index = 0;
        this.chunkedMode = false;
    }

    ///..
    public StreamWriter(final OutputStream outputStream) {

        this(outputStream, 8192);
    }

    ///
    public void write(final CharSequence data) throws IOException {

        final int length = data.length();
        int dataIndex = 0;

        while(true) {

            int amountPossible = this.buffer.length - this.index;

            while(amountPossible >= 2 && dataIndex < length) {

                amountPossible -= this.writeChar(data.charAt(dataIndex++));
            }

            if(dataIndex >= length) break;
            else this.flush();
        }
    }

    ///..
    public void writeForObservability(final CharSequence data) throws IOException {

        final int length = data.length();
        int dataIndex = 0;

        while(true) {

            int amountPossible = this.buffer.length - this.index;

            while(amountPossible >= 2 && dataIndex < length) {

                final char currentChar = data.charAt(dataIndex++);
                int written;

                switch(currentChar) {

                    case '\n':

                        this.buffer[this.index++] = ApplicationProperties.NEWLINE_REPLACEMENT;
                        written = 1;

                    break;

                    case ApplicationProperties.FIELD_SEPARATOR:

                        this.buffer[this.index++] = ApplicationProperties.FIELD_SEPARATOR_REPLACEMENT;
                        written = 1;

                    break;

                    default: written = this.writeChar(currentChar); break;
                }

                amountPossible -= written;
            }

            if(dataIndex >= length) break;
            else this.flush();
        }
    }

    ///..
    public void write(final char data) throws IOException {

        if(this.index >= this.buffer.length - 1) this.flush();
        this.writeChar(data);
    }

    ///..
    public void startChunked() throws IOException {

        this.flush();
        this.chunkedMode = true;
    }

    ///..
    public void endChunked() throws IOException {

        if(this.index > 0) this.flush();

        this.flush();
        this.chunkedMode = false;
    }

    ///..
    @Override
    public void write(final int data) throws IOException {

        if(this.index >= this.buffer.length) this.flush();
        this.buffer[this.index++] = (byte)(data & 0x000000FF);
    }

    ///..
    @Override
    public void write(final byte[] data, final int start, final int length) throws IOException {

        int dataIndex = start;

        while(true) {

            final int amount = Math.min(this.buffer.length - this.index, length - dataIndex);

            System.arraycopy(data, dataIndex, this.buffer, this.index, amount);
            dataIndex += amount;
            this.index += amount;

            if(dataIndex >= length) break;
            else this.flush();
        }
    }

    ///..
    @Override
    public void flush() throws IOException {

        if(this.chunkedMode) {

            this.outputStream.write((Integer.toHexString(this.index) + "\r\n").getBytes());
            this.outputStream.write(buffer, 0, this.index);
            this.outputStream.write(ApplicationProperties.NEW_LINE_BYTES);
        }

        else {

            this.outputStream.write(buffer, 0, this.index);
        }

        this.index = 0;
    }

    ///..
    @Override
    public void close() throws IOException {

        this.flush();
        this.outputStream.close();
    }

    ///.
    private int writeChar(final char data) {

        if(data <= 0x007F) {

            this.buffer[this.index++] = (byte)(data & 0x00FF);
            return 1;
        }

        else {

            this.buffer[this.index++] = (byte)((data & 0xFF00) >> 8);
            this.buffer[this.index++] = (byte)(data & 0x00FF);

            return 2;
        }
    }

    ///
}
