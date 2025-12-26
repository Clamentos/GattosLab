package io.github.clamentos.gattoslab.web;

///
import io.github.clamentos.gattoslab.observability.logging.SquashedLogContainer;
import io.github.clamentos.gattoslab.observability.logging.log_squash.SquashLogEventType;

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
    private final SquashedLogContainer squashedLogContainer;

    ///..
    private final WebsiteResource notFoundResource;

    ///
    @Autowired
    public WebsiteController(final Website staticSite, final SquashedLogContainer squashedLogContainer) throws PropertyNotFoundException {

        this.website = staticSite;
        this.squashedLogContainer = squashedLogContainer;

        notFoundResource = staticSite.getContent("/errors/not-found.html");
    }

    ///
    @RequestMapping(path = "/{*spring}") // Match anything, except other defined controllers.
    public ResponseEntity<Object> serveContent(

        @PathVariable("spring") final String path,
        @RequestHeader(value = "If-Modified-Since", required = false) final String ifModifiedSince,
        @RequestAttribute("REQUEST_METHOD_ATTRIBUTE") final String requestMethod
    ) {

        final WebsiteResource content = website.getContent(path);
        if(content == null) return website.buildResponseForStaticContent(HttpStatus.NOT_FOUND, notFoundResource);

        if(requestMethod.equals("GET")) {

            if(ifModifiedSince != null && !ifModifiedSince.isEmpty()) {

                try {

                    final OffsetDateTime date = OffsetDateTime.parse(ifModifiedSince, DateTimeFormatter.RFC_1123_DATE_TIME);
                    if(date.compareTo(website.getTimeAtStartup()) > 0) return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
                }

                catch(final DateTimeParseException _) {

                    // Continue without caching.
                    squashedLogContainer.squash(SquashLogEventType.IF_MODIFIED_SINCE_HEADER_MALFORMED, null);
                }
            }

            return website.buildResponseForStaticContent(HttpStatus.OK, content);
        }

        else {

            final StringBuilder message = new StringBuilder("Method ");

            message.append(requestMethod);
            message.append(" is not supported for this endpoint.");

            if(content.isApi()) message.append(" Supported methods are: ").append(content.getSupportedMethods());
            else message.append(" Supported methods are: [GET]");

            final ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.METHOD_NOT_ALLOWED);

            problemDetail.setTitle("HTTP method not allowed");
            problemDetail.setDetail(message.toString());
            problemDetail.setInstance(URI.create(path));

            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).header("Content-Type", "application/json").body(problemDetail);
        }
    }

    ///
}
