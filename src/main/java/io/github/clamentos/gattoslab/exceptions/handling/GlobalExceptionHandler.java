package io.github.clamentos.gattoslab.exceptions.handling;

///
import com.mongodb.MongoException;

///.
import io.github.clamentos.gattoslab.configuration.PropertyProvider;
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.exceptions.RedirectException;
import io.github.clamentos.gattoslab.exceptions.TooManyRequestsException;

///.
import jakarta.el.PropertyNotFoundException;
import jakarta.servlet.http.HttpServletRequest;

///.
import java.net.URI;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

///
@ControllerAdvice

///
public final class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    ///
    private final String retryAfter;

    ///
    @Autowired
    public GlobalExceptionHandler(final PropertyProvider propertyProvider) throws PropertyNotFoundException {

        retryAfter = Integer.toString(propertyProvider.getProperty("app.ratelimit.retryAfter", Integer.class) / 1000);
    }

    ///
    @ExceptionHandler(value = ApiSecurityException.class, produces = "application/json")
    public ResponseEntity<ProblemDetail> handleApiSecurityException(final ApiSecurityException exc, final WebRequest request) {

        final ProblemDetail problemDetail = this.createDetail(HttpStatus.UNAUTHORIZED, "Unauthorized", exc.getMessage(), request);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    ///..
    @ExceptionHandler(value = MongoException.class, produces = "application/json")
    public ResponseEntity<ProblemDetail> handleMongoException(final MongoException exc, final WebRequest request) {

        final ProblemDetail problemDetail = this.createDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", exc.getMessage(), request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    ///..
    @ExceptionHandler(value = RedirectException.class)
    public ResponseEntity<Void> handleRedirectException(final RedirectException exc, final WebRequest request) {

        return ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT).header("Location", exc.getMessage()).build();
    }

    ///..
    @ExceptionHandler(value = TooManyRequestsException.class, produces = "application/json")
    public ResponseEntity<ProblemDetail> handleTooManyRequestsException(final TooManyRequestsException exc, final WebRequest request) {

        final ProblemDetail problemDetail = this.createDetail(HttpStatus.TOO_MANY_REQUESTS, "Rate limit triggered", exc.getMessage(), request);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", retryAfter).body(problemDetail);
    }

    ///.
    @Override
	protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(

        final HttpMediaTypeNotSupportedException exc,
        final HttpHeaders headers,
        final HttpStatusCode status,
        final WebRequest request
    ) {

        final ProblemDetail problemDetail = this.createDetail(HttpStatus.BAD_REQUEST, "Media type not supported", exc.getMessage(), request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).header("Content-Type", "application/json").body(problemDetail);
	}

    ///..
    @Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(

        final HttpMessageNotReadableException exc,
        final HttpHeaders headers,
        final HttpStatusCode status,
        final WebRequest request
    ) {

		final ProblemDetail problemDetail = this.createDetail(HttpStatus.BAD_REQUEST, "Malformed request", exc.getMessage(), request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).header("Content-Type", "application/json").body(problemDetail);
	}

    ///.
    private ProblemDetail createDetail(final HttpStatus httpStatus, final String title, final String message, final WebRequest request) {

        final HttpServletRequest httpServletRequest = ((ServletWebRequest) request).getRequest();
        final ProblemDetail problemDetail = ProblemDetail.forStatus(httpStatus);

        problemDetail.setTitle(title);
        problemDetail.setDetail(message);
        problemDetail.setInstance(URI.create(httpServletRequest.getRequestURI()));

        return problemDetail;
    }

    ///
}
