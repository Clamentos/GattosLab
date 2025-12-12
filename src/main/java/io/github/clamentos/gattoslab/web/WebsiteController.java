package io.github.clamentos.gattoslab.web;

///
import io.github.clamentos.gattoslab.configuration.PropertyProvider;

///.
import jakarta.el.PropertyNotFoundException;

///.
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

///
@RestController
@Slf4j

///
public final class WebsiteController {

    ///
    private final Website website;

    ///
    @Autowired
    public WebsiteController(final Website staticSite, final PropertyProvider propertyProvider) throws PropertyNotFoundException {

        this.website = staticSite;
    }

    ///
    @RequestMapping(path = "/{*spring}") // Match anything, except other defined controllers.
    public ResponseEntity<Object> serveContent(

        @PathVariable("spring") final String path,
        @RequestHeader(value = "If-Modified-Since", required = false) final String ifModifiedSince,
        @RequestAttribute("REQUEST_METHOD") final String requestMethod
    ) {

        final WebsiteResource content = website.getContent(path);
        if(content == null) return website.buildResponseForStaticContent(HttpStatus.NOT_FOUND, website.getContent("/errors/not-found.html"));

        if(requestMethod.equals("GET")) {

            if(ifModifiedSince != null) {

                try {

                    final OffsetDateTime date = OffsetDateTime.parse(ifModifiedSince, DateTimeFormatter.RFC_1123_DATE_TIME);
                    if(date.compareTo(website.getTimeAtStartup()) > 0) return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
                }

                catch(final DateTimeParseException exc) {

                    // Continue without caching.
                    log.warn("Date-time parse exception for header If-Modified-Since: {}", exc);
                }
            }

            return website.buildResponseForStaticContent(HttpStatus.OK, content);
        }

        else {

            String message = "Method " + requestMethod + " is not supported for this endpoint.";

            if(content.isApi()) message += "Supported methods are: " + content.getSupportedMethods();
            else message += "Supported methods are: [GET]";

            final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.METHOD_NOT_ALLOWED);

            problemDetail.setTitle("HTTP method not allowed");
            problemDetail.setDetail(message);
            problemDetail.setInstance(URI.create(path));

            return ResponseEntity

                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .header("Content-Type", "application/json")
                .body(problemDetail)
            ;
        }
    }

    ///
}
