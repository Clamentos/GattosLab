package io.github.clamentos.gattoslab.http.server;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exchange.handling.components.Resource;
import io.github.clamentos.gattoslab.http.HttpHeader;
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
import java.util.Locale;
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

    private final byte[] remoteIpAddress;

    private final HttpMethod method;
    private final String uri;

    private final Map<HttpHeader, String> requestHeaders;
    private final Map<HttpHeader, String> responseHeaders;

    private final SocketReader reader;
    private final StreamWriter writer;

    ///..
    @Setter private Resource resource;
    @Setter private boolean isUserAgentBlocked;

    ///..
    private HttpStatus responseStatus;

    ///..
    private final boolean isKeepAlive;

    @Setter private boolean isTracked;
    private boolean isHandled;
    private boolean doForceClose;

    ///
    public HttpExchange(

        final long requestId,
        final byte[] remoteIpAddress,
        final HttpMethod method,
        final String uri,
        final Map<HttpHeader, String> requestHeaders,
        final SocketReader reader,
        final StreamWriter writer
    ) {

        this.requestId = requestId;
        this.startTime = System.currentTimeMillis();

        this.remoteIpAddress = remoteIpAddress;

        this.method = method;
        this.uri = uri;

        this.requestHeaders = requestHeaders;
        this.responseHeaders = new EnumMap<>(HttpHeader.class);

        this.reader = reader;
        this.writer = writer;

        this.resource = null;
        this.isUserAgentBlocked = false;

        this.responseStatus = null;

        this.isTracked = false;
        this.isHandled = false;
        this.doForceClose = false;

        final String connectionHeader = this.requestHeaders.get(HttpHeader.CONNECTION);

        if(connectionHeader != null && connectionHeader.toLowerCase(Locale.US).contains("close")) {

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
        final Map<HttpHeader, String> headers,
        final MimeType mimeType,
        final Object body

    ) throws IOException {

        this.responseHeaders.putAll(ApplicationProperties.EXTRA_HEADERS);
        this.responseHeaders.putAll(headers);

        this.responseHeaders.put(

            HttpHeader.DATE,
            DateTimeFormatter.RFC_1123_DATE_TIME.format(OffsetDateTime.ofInstant(Instant.ofEpochMilli(this.startTime), GenericUtils.DEFAULT_ZONE_ID))
        );

        if(this.doForceClose) this.responseHeaders.putAll(ApplicationProperties.CLOSE_CONNECTION_HEADER);
        if(mimeType != null) this.responseHeaders.putAll(mimeType.getValueForResponse());

        if(body != null) {

            if(body instanceof final byte[] byteBody) {

                this.responseHeaders.put(HttpHeader.CONTENT_LENGTH, Integer.toString(byteBody.length));
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

        for(final Entry<HttpHeader, String> header : this.responseHeaders.entrySet()) {

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

        this.doForceClose = true;
    }

    ///
}
