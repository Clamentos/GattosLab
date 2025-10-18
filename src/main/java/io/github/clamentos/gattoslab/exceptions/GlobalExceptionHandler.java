package io.github.clamentos.gattoslab.exceptions;

///
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

///
@ControllerAdvice
@Slf4j

///
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    ///
    private final String retryAfter;

    ///
    @Autowired
    public GlobalExceptionHandler(final Environment environment) {

        retryAfter = environment.getProperty("app.ratelimit.retryAfter", String.class, "60");
    }

    ///
    @ExceptionHandler(value = TooManyRequestsException.class)
    public ResponseEntity<Void> handleTooManyRequestsException(final TooManyRequestsException exc, final WebRequest request) {

        log.warn("Rate limit, {}", exc.getIp());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", retryAfter).build();
    }

    ///
}
