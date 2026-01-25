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
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

///..
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

///
@ControllerAdvice

///
public final class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    ///
    private final String retryAfter;

    ///
    @Autowired
    public GlobalExceptionHandler(@NonNull final PropertyProvider propertyProvider) throws PropertyNotFoundException {

        retryAfter = Integer.toString(propertyProvider.getProperty("app.ratelimit.retryAfter", Integer.class) / 1000);
    }

    ///
    @ExceptionHandler(value = ApiSecurityException.class, produces = "application/json")
    public @NonNull ResponseEntity<ProblemDetail> handleApiSecurityException(@NonNull final ApiSecurityException exc, @NonNull final WebRequest request) {

        final ProblemDetail problemDetail = this.createDetail(HttpStatus.UNAUTHORIZED, "Unauthorized", exc.getMessage(), request);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    ///..
    @ExceptionHandler(value = Exception.class, produces = "application/json")
    public @NonNull ResponseEntity<ProblemDetail> handleAllOtherExceptions(@NonNull final Exception exc, @NonNull final WebRequest request) {

        final ProblemDetail problemDetail = this.createDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", exc.getMessage(), request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    ///..
    @ExceptionHandler(value = MongoException.class, produces = "application/json")
    public @NonNull ResponseEntity<ProblemDetail> handleMongoException(@NonNull final MongoException exc, @NonNull final WebRequest request) {

        final ProblemDetail problemDetail = this.createDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Database error", exc.getMessage(), request);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    ///..
    @ExceptionHandler(value = MultipartException.class, produces = "application/json")
    public @NonNull ResponseEntity<ProblemDetail> handleMultipartException(@NonNull final MultipartException exc, @NonNull final WebRequest request) {

        final ProblemDetail problemDetail = this.createDetail(HttpStatus.BAD_REQUEST, "Uploads not supported", exc.getMessage(), request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    ///..
    @ExceptionHandler(RedirectException.class)
    public @NonNull ResponseEntity<Void> handleRedirectException(@NonNull final RedirectException exc, @Nullable final WebRequest request) {

        return ResponseEntity.status(HttpStatus.PERMANENT_REDIRECT).header("Location", exc.getMessage()).build();
    }

    ///..
    @ExceptionHandler(value = TooManyRequestsException.class, produces = "application/json")
    public @NonNull ResponseEntity<ProblemDetail> handleTooManyRequestsException(@NonNull final TooManyRequestsException exc, @NonNull final WebRequest request) {

        final ProblemDetail problemDetail = this.createDetail(HttpStatus.TOO_MANY_REQUESTS, "Rate limit triggered", exc.getMessage(), request);
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", retryAfter).body(problemDetail);
    }

    ///.
    @Override
	protected @NonNull ResponseEntity<Object> handleHttpMediaTypeNotSupported(

        @NonNull final HttpMediaTypeNotSupportedException exc,
        @Nullable final HttpHeaders headers,
        @Nullable final HttpStatusCode status,
        @NonNull final WebRequest request
    ) {

        final ProblemDetail problemDetail = this.createDetail(HttpStatus.BAD_REQUEST, "Media type not supported", exc.getMessage(), request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).header("Content-Type", "application/json").body(problemDetail);
	}

    ///..
    @Override
	protected @NonNull ResponseEntity<Object> handleHttpMessageNotReadable(

        @NonNull final HttpMessageNotReadableException exc,
        @Nullable final HttpHeaders headers,
        @Nullable final HttpStatusCode status,
        @NonNull final WebRequest request
    ) {

		final ProblemDetail problemDetail = this.createDetail(HttpStatus.BAD_REQUEST, "Malformed request", exc.getMessage(), request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).header("Content-Type", "application/json").body(problemDetail);
	}

    ///.
    private @NonNull ProblemDetail createDetail(

        @NonNull final HttpStatus httpStatus,
        @NonNull final String title,
        @NonNull final String message,
        @NonNull final WebRequest request
    ) {

        final HttpServletRequest httpServletRequest = ((ServletWebRequest) request).getRequest();
        final ProblemDetail problemDetail = ProblemDetail.forStatus(httpStatus);

        problemDetail.setType(URI.create("about:custom_error"));
        problemDetail.setTitle(title);
        problemDetail.setDetail(message);
        problemDetail.setInstance(URI.create(httpServletRequest.getRequestURI()));

        return problemDetail;
    }

    ///
}
