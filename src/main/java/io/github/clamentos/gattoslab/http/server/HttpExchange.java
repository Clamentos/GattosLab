package io.github.clamentos.gattoslab.http.server;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exchange.handling.components.Resource;
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;

///..
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
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

    private final Map<HttpHeader, String> requestHeaders;
    private final Map<HttpHeader, byte[]> responseHeaders;

    private final SocketReader requestBody;
    private final OutputStream outputStream;

    ///..
    @Setter
    private Resource resource;

    @Setter
    private boolean isTracked;

    private boolean isHandled;
    private HttpStatus responseStatus;
    private boolean forceClose;

    ///
    public HttpExchange(

        final long requestId,
        final byte[] remoteAddress,
        final HttpMethod method,
        final String uri,
        final Map<HttpHeader, String> requestHeaders,
        final SocketReader requestBody,
        final OutputStream outputStream
    ) {

        this.requestId = requestId;
        this.startTime = System.currentTimeMillis();

        this.remoteAddress = remoteAddress;

        this.method = method;
        this.uri = uri;

        final int queryIdx = uri.indexOf('?');
        this.path = queryIdx > 0 ? uri.substring(0, queryIdx) : uri;

        this.requestHeaders = requestHeaders;
        this.responseHeaders = new EnumMap<>(HttpHeader.class);

        this.requestBody = requestBody;
        this.outputStream = outputStream;

        this.resource = null;
        this.isTracked = false;
        this.isHandled = false;
        this.responseStatus = null;
        this.forceClose = false;
    }

    ///
    public void respond(final HttpStatus status, final Map<HttpHeader, byte[]> headers, final MimeType mimeType, final Object body)
    throws IOException {

        this.responseHeaders.putAll(ApplicationProperties.EXTRA_HEADERS);
        this.responseHeaders.putAll(ApplicationProperties.NO_CACHE_HEADERS);
        this.responseHeaders.putAll(headers);

        if(mimeType != null) this.responseHeaders.put(HttpHeader.CONTENT_TYPE, mimeType.getValueForResponse());

        if(body != null) {

            if(body instanceof final byte[] byteBody) {

                this.responseHeaders.put(HttpHeader.CONTENT_LENGTH, Integer.toString(byteBody.length).getBytes());
            }

            else if(body instanceof ResponseBodyCallback) {

                this.responseHeaders.putAll(ApplicationProperties.TRANSFER_CHUNKED_HEADERS);
            }
        }

        else {

            this.responseHeaders.put(HttpHeader.CONTENT_LENGTH, "0".getBytes());
        }

        this.responseStatus = status;
        this.outputStream.write(status.getValueForResponse());

        for(final Entry<HttpHeader, byte[]> header : this.responseHeaders.entrySet()) {

            this.outputStream.write(header.getKey().getValueForResponse());
            this.outputStream.write(header.getValue());
            this.outputStream.write("\r\n".getBytes());
        }

        this.outputStream.write("\r\n".getBytes());

        if(body != null) {

            if(body instanceof final byte[] byteBody) this.outputStream.write(byteBody);
            else if(body instanceof final ResponseBodyCallback bodyCallback) bodyCallback.writeBody(this.outputStream);
        }

        this.outputStream.flush();
        this.isHandled = true;
    }

    ///..
    @Override
    public void close() {

        this.forceClose = true;
    }

    ///
}
