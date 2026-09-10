package io.github.clamentos.gattoslab.website;

///
import com.sun.net.httpserver.HttpExchange;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exchange.handling.BasicHandler;
import io.github.clamentos.gattoslab.exchange.handling.ExceptionHandler;
import io.github.clamentos.gattoslab.exchange.handling.components.Resource;
import io.github.clamentos.gattoslab.exchange.handling.components.StaticResource;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.observability.ObservabilityService;

///..
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

///
public final class WebsiteHandler extends BasicHandler {

    ///
    public WebsiteHandler(final ObservabilityService observabilityService, final ExceptionHandler exceptionHandler) {

        super(observabilityService, exceptionHandler);
    }

    ///
    @Override
    protected void doHandle(final HttpExchange exchange) {

        final Resource resource = (Resource)exchange.getAttribute(ApplicationProperties.REQUEST_RESOURCE_ATTRIBUTE);

        if(resource == null) {

            super.respondNotFound(exchange);
            return;
        }

        if(super.rejectMethodNotAllowed(exchange, HttpMethod.GET)) return;

        final StaticResource staticResource = (StaticResource)resource;
        final String diskPath = staticResource.getDiskPath();

        final Map<String, List<String>> headers = staticResource.isCacheable() ?

            ApplicationProperties.GZIP_CACHE_HEADERS :
            ApplicationProperties.GZIP_HEADERS
        ;

        if(diskPath != null) {

            final InputStream rawDiskData = WebsiteHandler.class.getClassLoader().getResourceAsStream(diskPath);

            if(rawDiskData == null) {

                super.respondNotFound(exchange);
                return;
            }

            super.respond(

                exchange,
                HttpStatus.OK,
                headers,
                staticResource.getMimeType(),

                () -> {

                    final GZIPOutputStream compressor = new GZIPOutputStream(exchange.getResponseBody());

                    rawDiskData.transferTo(compressor);
                    compressor.finish();
                    rawDiskData.close();
                }
            );
        }

        else {

            super.respond(exchange, HttpStatus.OK, headers, staticResource.getMimeType(), staticResource.getCompressedContent());
        }
    }

    ///
}
