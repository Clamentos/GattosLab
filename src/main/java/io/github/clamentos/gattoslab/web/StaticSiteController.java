package io.github.clamentos.gattoslab.web;

///
import io.github.clamentos.gattoslab.utils.Pair;
import io.github.clamentos.gattoslab.utils.PropertyProvider;

///.
import jakarta.el.PropertyNotFoundException;

///.
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

///
@RestController
@Slf4j

///
public final class StaticSiteController {

    ///
    private final String notFoundPath;

    ///..
    private final StaticSite staticSite;

    ///
    @Autowired
    public StaticSiteController(final StaticSite staticSite, final PropertyProvider propertyProvider) throws PropertyNotFoundException {

        notFoundPath = propertyProvider.getProperty("app.site.notFoundPath", String.class);
        this.staticSite = staticSite;
    }

    ///
    @GetMapping(path = "/{*spring}") // Match anything, except other defined controllers.
    public ResponseEntity<byte[]> serveContent(

        @PathVariable("spring") final String path,
        @RequestHeader(value = "If-Modified-Since", required = false) final String ifModifiedSince
    ) {

        if(ifModifiedSince != null) {

            try {

                final OffsetDateTime date = OffsetDateTime.parse(ifModifiedSince, DateTimeFormatter.RFC_1123_DATE_TIME);
                if(date.compareTo(staticSite.getTimeAtStartup()) > 0) return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }

            catch(final DateTimeParseException exc) {

                // Continue without caching.
                log.warn("Date-time parse exception for header If-Modified-Since. {}", exc);
            }
        }

        HttpStatus status = HttpStatus.OK;
        Pair<String, byte[]> content = staticSite.getContent(path);

        if(content == null) {

            content = staticSite.getContent(notFoundPath);
            status = HttpStatus.NOT_FOUND;
        }

        return staticSite.buildSiteResponse(status, content);
    }

    ///
}
