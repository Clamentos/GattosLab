package io.github.clamentos.gattoslab.exceptions;

///
import com.mongodb.MongoException;

///.
import io.github.clamentos.gattoslab.utils.PropertyProvider;

///.
import jakarta.el.PropertyNotFoundException;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

///
@ControllerAdvice

///
public final class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    ///
    private final String baseUrl;
    private final String retryAfter;

    ///
    @Autowired
    public GlobalExceptionHandler(final PropertyProvider propertyProvider) throws PropertyNotFoundException {

        baseUrl = propertyProvider.getProperty("app.baseUrl", String.class) + propertyProvider.getProperty("server.port", String.class);
        retryAfter = Integer.toString(propertyProvider.getProperty("app.ratelimit.retryAfter", Integer.class) / 1000);
    }

    ///
    @ExceptionHandler(value = ApiSecurityException.class)
    public ResponseEntity<Void> handleApiSecurityException(final ApiSecurityException exc, final WebRequest request) {

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    ///..
    @ExceptionHandler(value = MongoException.class, produces = "text/html")
    public ResponseEntity<String> handleMongoException(final MongoException exc, final WebRequest request) {

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exc.getMessage());
    }

    ///..
    @ExceptionHandler(value = RedirectException.class, produces = "text/html")
    public ResponseEntity<String> handleRedirectException(final RedirectException exc, final WebRequest request) {

        final String redirectHtml = "<head><meta http-equiv=\"Refresh\" content=\"0; URL=" + baseUrl + exc.getMessage() + "\"/></head>";
        return ResponseEntity.ok(redirectHtml);
    }

    ///..
    @ExceptionHandler(value = TooManyRequestsException.class, produces = "text/plain")
    public ResponseEntity<String> handleTooManyRequestsException(final TooManyRequestsException exc, final WebRequest request) {

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", retryAfter).body(exc.getMessage());
    }

    ///
}
