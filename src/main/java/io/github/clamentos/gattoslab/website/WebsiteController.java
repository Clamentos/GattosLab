package io.github.clamentos.gattoslab.website;

///
import io.github.clamentos.gattoslab.exceptions.IllegalHttpMethodException;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpUtils;
import io.github.clamentos.gattoslab.observability.logging.SquashedLogsContainer;
import io.github.clamentos.gattoslab.observability.logging.squash.SquashLogEventType;

///..
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import io.undertow.util.StatusCodes;

///..
import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

///..
import lombok.extern.slf4j.Slf4j;

///
@Slf4j

///
public final class WebsiteController {

    ///
    private final Website website;
    private final SquashedLogsContainer squashedLogsContainer;

    ///..
    private final WebsiteResource notFoundResource;

    ///
    public WebsiteController(final Website staticSite, final SquashedLogsContainer squashedLogsContainer) {

        this.website = staticSite;
        this.squashedLogsContainer = squashedLogsContainer;

        notFoundResource = staticSite.getContent(Apis.FE_NOT_FOUND);
    }

    ///
    public void serveContent(final HttpServerExchange exchange) throws IllegalHttpMethodException {

        final WebsiteResource content = website.getContent(exchange.getRequestPath());

        if(content != null) {

            final HttpMethod requestMethod = exchange.getAttachment(HttpUtils.DECODED_HTTP_METHOD);

            if(requestMethod != HttpMethod.GET) {

                throw new IllegalHttpMethodException("WebsiteController.serveContent~Method " + requestMethod + " is not supported for this endpoint. Supported methods are: GET");
            }

            if(content.isCacheable()) {

                final String ifModifiedSince = HttpUtils.getHeaderValue(exchange.getRequestHeaders(), Headers.IF_MODIFIED_SINCE_STRING);

                if(ifModifiedSince != null && !ifModifiedSince.isEmpty()) {

                    try {

                        final OffsetDateTime date = OffsetDateTime.parse(ifModifiedSince, DateTimeFormatter.RFC_1123_DATE_TIME);

                        if(date.compareTo(website.getTimeAtStartup()) > 0) {

                            exchange.setStatusCode(StatusCodes.NOT_MODIFIED);
                            return;
                        }
                    }

                    catch(final DateTimeParseException _) {

                        squashedLogsContainer.squash(SquashLogEventType.IF_MODIFIED_SINCE_HEADER_MALFORMED, null);
                    }
                }
            }

            this.buildResponseForStaticContent(StatusCodes.OK, content, exchange);
        }

        else {

            this.buildResponseForStaticContent(StatusCodes.NOT_FOUND, notFoundResource, exchange);
        }
    }

    ///.
    public void buildResponseForStaticContent(final int status, final WebsiteResource resource, final HttpServerExchange exchange) {

        exchange.setStatusCode(status);

        final HeaderMap headers = exchange.getResponseHeaders();

        HttpUtils.addContentType(headers, resource.getMimeType());
        HttpUtils.addCache(headers, website.getCacheDuration());
        HttpUtils.addLastModified(headers, website.getTimeAtStartupStr());
        HttpUtils.addGzipEncoding(headers);

        exchange.getResponseSender().send(ByteBuffer.wrap(resource.getContent()));
    }

    ///
}
