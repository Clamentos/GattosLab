package io.github.clamentos.gattoslab.server.http.exchange.components;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.configuration.components.HttpProxy;
import io.github.clamentos.gattoslab.datastructures.MutableString;
import io.github.clamentos.gattoslab.datastructures.StreamWriter;
import io.github.clamentos.gattoslab.datastructures.addresses.IpAddress;
import io.github.clamentos.gattoslab.server.components.SocketReader;
import io.github.clamentos.gattoslab.server.http.exchange.components.body.ChunkedResponseBodyCallback;
import io.github.clamentos.gattoslab.server.http.exchange.components.body.ResponseBodyCallback;
import io.github.clamentos.gattoslab.server.http.exchange.components.resources.Resource;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.IOException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

///..
import lombok.Getter;
import lombok.Setter;

///
@Getter

///
public final class HttpExchange {

    ///
    private final long startTime;

    ///..
    private final IpAddress clientRemoteAddress;
    private final HttpMethod method;
    private final String uri;

    ///..
    private final Map<HttpHeader, String> requestHeaders;
    private final Map<HttpHeader, String> responseHeaders;

    ///..
    private final SocketReader reader;
    private final StreamWriter writer;

    ///..
    @Setter
    private String domain;

    @Setter
    private HttpProxy proxy;

    @Setter
    private Resource resource;

    ///..
    private HttpStatus responseStatus;
    private long endTime;

    ///..
    @Setter
    private boolean isUriBad;

    @Setter
    private boolean isTracked;

    private boolean isHandled;
    private boolean doForceClose;

    ///
    public HttpExchange(

        final IpAddress clientRemoteAddress,
        final HttpMethod method,
        final String uri,
        final Map<HttpHeader, String> requestHeaders,
        final SocketReader reader,
        final StreamWriter writer
    ) {

        this.startTime = System.currentTimeMillis();

        this.clientRemoteAddress = clientRemoteAddress;
        this.method = method;
        this.uri = uri;

        this.requestHeaders = requestHeaders;
        this.responseHeaders = new EnumMap<>(HttpHeader.class);

        this.reader = reader;
        this.writer = writer;

        this.domain = null;
        this.proxy = null;
        this.resource = null;

        this.responseStatus = null;
        this.endTime = this.startTime;

        this.isUriBad = false;
        this.isTracked = false;
        this.isHandled = false;
        this.doForceClose = false;
    }

    ///
    public void respond(final HttpStatus status, final Map<HttpHeader, String> headers, final MimeType mimeType, final Object body)
    throws IOException {

        this.responseHeaders.putAll(ApplicationProperties.EXTRA_HEADERS);
        if(mimeType != null) this.responseHeaders.put(HttpHeader.CONTENT_TYPE, mimeType.getResponseValue());

        if(this.doForceClose) {

            this.responseHeaders.remove(HttpHeader.KEEP_ALIVE);
            this.responseHeaders.putAll(ApplicationProperties.CLOSE_CONNECTION_HEADER);
        }

        else {

            this.responseHeaders.putAll(ApplicationProperties.KEEP_ALIVE_HEADERS);
        }

        this.responseHeaders.putAll(headers);

        if(body != null) {

            if(body instanceof final byte[] byteBody) {

                this.responseHeaders.put(HttpHeader.CONTENT_LENGTH, Integer.toString(byteBody.length));
            }

            else if(body instanceof ChunkedResponseBodyCallback) {

                this.responseHeaders.putAll(ApplicationProperties.TRANSFER_CHUNKED_HEADER);
            }
        }

        else {

            this.responseHeaders.putAll(ApplicationProperties.NO_LENGTH_HEADER);
        }

        this.endTime = System.currentTimeMillis();
        this.responseHeaders.put(HttpHeader.DATE, this.formatEndTime());
        this.responseStatus = status;

        this.writer.write(status.getResponseValue());

        for(final Entry<HttpHeader, String> header : this.responseHeaders.entrySet()) {

            this.writer.write(header.getKey().getResponseValue());
            this.writer.write(header.getValue());
            this.writer.write(GenericUtils.NEW_LINE_BYTES);
        }

        this.writer.write(GenericUtils.NEW_LINE_BYTES);

        if(body != null) {

            if(body instanceof final byte[] byteBody) {

                this.writer.write(byteBody);
                this.writer.flush();
            }

            else if(body instanceof final ChunkedResponseBodyCallback bodyCallback) {

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
    public void respondForProxy(final CharSequence responseLine, final List<CharSequence> headers, final ResponseBodyCallback body)
    throws IOException {

        this.endTime = System.currentTimeMillis();
        this.responseStatus = HttpStatus.decode(Integer.parseInt(GenericUtils.fastSplit(responseLine, ' ').get(1)));

        this.writer.write(responseLine);
        this.writer.write(GenericUtils.NEW_LINE_BYTES);

        final int length = headers.size();

        int contentLength = 0;
        boolean flag = false;

        for(int i = 0; i < length; i++) {

            final CharSequence header = headers.get(i);
            final List<String> headerSplits = GenericUtils.fastSplit(header, ':');

            if(!flag && "Content-Length".equalsIgnoreCase(headerSplits.get(0))) {

                contentLength = Integer.parseInt(headerSplits.get(1).trim());
                flag = true;
            }

            this.writer.write(header);
            this.writer.write(GenericUtils.NEW_LINE_BYTES);
        }

        this.writer.write(GenericUtils.NEW_LINE_BYTES);
        body.writeBody(writer, contentLength);

        this.isHandled = true;
    }

    ///..
    public void flagForClose() {

        this.doForceClose = true;
    }

    ///..
    @Override
    public String toString() {

        return

            String.valueOf(this.method) + ApplicationProperties.EXCHANGE_STRING_SEPARATOR +
            GenericUtils.normalizedForObservability(String.valueOf(this.uri)) + ApplicationProperties.EXCHANGE_STRING_SEPARATOR +
            "Request headers " + this.redactSensitiveHeaders(this.requestHeaders) + ApplicationProperties.EXCHANGE_STRING_SEPARATOR +
            "Response headers " + this.redactSensitiveHeaders(this.responseHeaders)
        ;
    }

    ///.
    private String formatEndTime() {

        return DateTimeFormatter.RFC_1123_DATE_TIME.format(OffsetDateTime.ofInstant(

            Instant.ofEpochMilli(this.endTime),
            GenericUtils.DEFAULT_ZONE_ID
        ));
    }

    ///..
    private String redactSensitiveHeaders(final Map<HttpHeader, String> headers) {

        if(headers == null) return "null";
        final MutableString mutableString = new MutableString(128);

        mutableString.append('{');

        for(final Entry<HttpHeader, String> header : headers.entrySet()) {

            final HttpHeader key = header.getKey();

            mutableString.append(key.name());
            mutableString.append('=');

            if(key.isSensitive()) mutableString.append(ApplicationProperties.EXCHANGE_LOG_REDACTED_VALUE);
            else mutableString.append(GenericUtils.normalizedForObservability(header.getValue()));

            mutableString.append(", ");
        }

        if(!headers.isEmpty()) {

            mutableString.deleteLastCharUnsafe();
            mutableString.deleteLastCharUnsafe();
        }

        mutableString.append('}');
        return mutableString.toString();
    }

    ///
}
