package io.github.clamentos.gattoslab.website;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exchange.handling.BasicHandler;
import io.github.clamentos.gattoslab.exchange.handling.ExceptionHandler;
import io.github.clamentos.gattoslab.exchange.handling.components.StaticResource;
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.server.HttpExchange;
import io.github.clamentos.gattoslab.observability.ObservabilityService;

///..
import java.io.InputStream;
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

        final StaticResource resource = (StaticResource)exchange.getResource();
        if(super.rejectMethodNotAllowed(exchange, HttpMethod.GET)) return;

        final String diskPath = resource.getDiskPath();

        final Map<HttpHeader, String> headers = resource.isCacheable() ?

            ApplicationProperties.GZIP_CACHE_HEADERS :
            ApplicationProperties.GZIP_HEADER
        ;

        if(diskPath != null) {

            final InputStream rawDiskData = WebsiteHandler.class.getClassLoader().getResourceAsStream(diskPath);

            if(rawDiskData == null) {

                super.respond(exchange, HttpStatus.NOT_FOUND);
                return;
            }

            super.respond(

                exchange,
                HttpStatus.OK,
                headers,
                resource.getMimeType(),

                writer -> {

                    final GZIPOutputStream compressor = new GZIPOutputStream(writer);

                    rawDiskData.transferTo(compressor);
                    compressor.finish();
                    rawDiskData.close();
                }
            );
        }

        else {

            super.respond(exchange, HttpStatus.OK, headers, resource.getMimeType(), resource.getCompressedContent());
        }
    }

    ///
}
