package io.github.clamentos.gattoslab.web;

///
import io.github.clamentos.gattoslab.utils.Pair;

///.
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

///.
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

///
@RequiredArgsConstructor
@RestController
@Slf4j

///
public final class StaticSiteController {

    ///
    private final StaticSite staticSite;

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

            content = staticSite.getContent("/errors/not-found.html");
            status = HttpStatus.NOT_FOUND;
        }

        return staticSite.buildSiteResponse(status, content);
    }

    ///
}
