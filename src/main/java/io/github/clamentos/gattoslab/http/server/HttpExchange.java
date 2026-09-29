package io.github.clamentos.gattoslab.http.server;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exchange.handling.components.Resource;
import io.github.clamentos.gattoslab.http.HttpHeaderName;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

///..
import lombok.Getter;
import lombok.Setter;

///
@Getter

///
public final class HttpExchange implements Closeable {

    ///
    private final long requestId;
    private final long startTime;

    private final byte[] remoteAddress;

    private final HttpMethod method;
    private final String uri;
    private final String path;

    private final Map<HttpHeaderName, String> requestHeaders;
    private final Map<HttpHeaderName, String> responseHeaders;

    private final SocketReader reader;
    private final StreamWriter writer;

    ///..
    @Setter private Resource resource;
    private HttpStatus responseStatus;

    private final boolean isKeepAlive;

    @Setter private boolean isTracked;
    private boolean isHandled;
    private boolean forceClose;

    ///
    public HttpExchange(

        final long requestId,
        final byte[] remoteAddress,
        final HttpMethod method,
        final String uri,
        final Map<HttpHeaderName, String> requestHeaders,
        final SocketReader reader,
        final StreamWriter writer
    ) {

        this.requestId = requestId;
        this.startTime = System.currentTimeMillis();

        this.remoteAddress = remoteAddress;

        this.method = method;
        this.uri = uri;

        final int endOfPath = this.endOfPath(uri);
        this.path = endOfPath > 0 ? uri.substring(0, endOfPath) : uri;

        this.requestHeaders = requestHeaders;
        this.responseHeaders = new EnumMap<>(HttpHeaderName.class);

        this.reader = reader;
        this.writer = writer;

        this.resource = null;
        this.responseStatus = null;

        this.isTracked = false;
        this.isHandled = false;
        this.forceClose = false;

        final String connectionHeader = this.requestHeaders.get(HttpHeaderName.CONNECTION);

        if(connectionHeader != null && connectionHeader.contains("close")) {

            this.responseHeaders.putAll(ApplicationProperties.CLOSE_CONNECTION_HEADER);
            this.isKeepAlive = false;
        }

        else {

            this.responseHeaders.putAll(ApplicationProperties.KEEP_ALIVE_HEADERS);
            this.isKeepAlive = true;
        }
    }

    ///
    public void respond(

        final HttpStatus status,
        final Map<HttpHeaderName, String> headers,
        final MimeType mimeType,
        final Object body

    ) throws IOException {

        this.responseHeaders.putAll(ApplicationProperties.EXTRA_HEADERS);
        this.responseHeaders.putAll(headers);

        this.responseHeaders.put(

            HttpHeaderName.DATE,
            DateTimeFormatter.RFC_1123_DATE_TIME.format(OffsetDateTime.ofInstant(Instant.ofEpochMilli(this.startTime), GenericUtils.DEFAULT_ZONE_ID))
        );

        if(this.forceClose) this.responseHeaders.putAll(ApplicationProperties.CLOSE_CONNECTION_HEADER);
        if(mimeType != null) this.responseHeaders.putAll(mimeType.getValueForResponse());

        if(body != null) {

            if(body instanceof final byte[] byteBody) {

                this.responseHeaders.put(HttpHeaderName.CONTENT_LENGTH, Integer.toString(byteBody.length));
            }

            else if(body instanceof ResponseBodyCallback) {

                this.responseHeaders.putAll(ApplicationProperties.TRANSFER_CHUNKED_HEADER);
            }
        }

        else {

            this.responseHeaders.putAll(ApplicationProperties.NO_LENGTH_HEADER);
        }

        this.responseStatus = status;
        this.writer.write(status.getValueForResponse());

        for(final Entry<HttpHeaderName, String> header : this.responseHeaders.entrySet()) {

            this.writer.write(header.getKey().getValueForResponse());
            this.writer.write(header.getValue());
            this.writer.write(ApplicationProperties.NEW_LINE_BYTES);
        }

        this.writer.write(ApplicationProperties.NEW_LINE_BYTES);

        if(body != null) {

            if(body instanceof final byte[] byteBody) {

                this.writer.write(byteBody);
                this.writer.flush();
            }

            else if(body instanceof final ResponseBodyCallback bodyCallback) {

                this.writer.startChunked();
                bodyCallback.writeBody(this.writer);
                this.writer.endChunked();
            }
        }

        else {

            this.writer.flush();
        }

        this.isHandled = true;
    }

    ///..
    @Override
    public void close() {

        this.forceClose = true;
    }

    ///.
    private int endOfPath(final String uri) {

        if(uri == null) return -1;
        final int length = uri.length();

        for(int i = 0; i < length; i++) {

            final char currentChar = uri.charAt(i);
            if(currentChar == '?' || currentChar == '#') return i;
        }

        return -1;
    }

    ///
}
