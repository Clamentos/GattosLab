package io.github.clamentos.gattoslab.website;

///
import io.github.clamentos.gattoslab.exceptions.IllegalHttpMethodException;
import io.github.clamentos.gattoslab.observability.logging.SquashedLogContainer;
import io.github.clamentos.gattoslab.observability.logging.squash.SquashLogEventType;
import io.github.clamentos.gattoslab.utils.HttpMethod;
import io.github.clamentos.gattoslab.utils.HttpUtils;

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
    private final SquashedLogContainer squashedLogContainer;

    ///..
    private final WebsiteResource notFoundResource;

    ///
    public WebsiteController(final Website staticSite, final SquashedLogContainer squashedLogContainer) {

        this.website = staticSite;
        this.squashedLogContainer = squashedLogContainer;

        notFoundResource = staticSite.getContent("/errors/not-found.html");
    }

    ///
    public void serveContent(final HttpServerExchange exchange) throws IllegalHttpMethodException {

        final String requestMethod = exchange.getRequestMethod().toString();

        if(!requestMethod.equals(HttpMethod.GET.name())) {

            throw new IllegalHttpMethodException("Method " + requestMethod + " is not supported for this endpoint. Supported methods are: GET");
        }

        final String ifModifiedSince = HttpUtils.getHeaderValue(exchange.getRequestHeaders(), Headers.IF_MODIFIED_SINCE_STRING);
        final WebsiteResource content = website.getContent(exchange.getRequestPath());

        if(content != null) {

            if(ifModifiedSince != null && !ifModifiedSince.isEmpty()) {

                try {

                    final OffsetDateTime date = OffsetDateTime.parse(ifModifiedSince, DateTimeFormatter.RFC_1123_DATE_TIME);

                    if(date.compareTo(website.getTimeAtStartup()) > 0) {

                        exchange.setStatusCode(StatusCodes.NOT_MODIFIED);
                        return;
                    }
                }

                catch(final DateTimeParseException _) {

                    // Continue without caching.
                    squashedLogContainer.squash(SquashLogEventType.IF_MODIFIED_SINCE_HEADER_MALFORMED, null);
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
        HttpUtils.addLastModified(headers, website.getTimeAtStartup());
        HttpUtils.addGzipEncoding(headers);

        exchange.getResponseSender().send(ByteBuffer.wrap(resource.getContent()));
    }

    ///
}
