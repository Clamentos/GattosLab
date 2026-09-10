package io.github.clamentos.gattoslab.http;

///
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter

///
public enum HttpStatus {

    ///
    OK(200),
    NO_CONTENT(204),
    SEE_OTHER(303),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    TOO_MANY_REQUESTS(429),
    INTERNAL_SERVER_ERROR(500),
    BAD_GATEWAY(502),
    SERVICE_UNAVAILABLE(503),
    GATEWAY_TIMEOUT(504),

    ///..
    TRUNCATED(599);

    ///
    public static HttpStatus decode(final int code) {

        switch(code) {

            case 200: return HttpStatus.OK;
            case 204: return HttpStatus.NO_CONTENT;
            case 303: return HttpStatus.SEE_OTHER;
            case 400: return HttpStatus.BAD_REQUEST;
            case 401: return HttpStatus.UNAUTHORIZED;
            case 403: return HttpStatus.FORBIDDEN;
            case 404: return HttpStatus.NOT_FOUND;
            case 405: return HttpStatus.METHOD_NOT_ALLOWED;
            case 429: return HttpStatus.TOO_MANY_REQUESTS;
            case 500: return HttpStatus.INTERNAL_SERVER_ERROR;
            case 502: return HttpStatus.BAD_GATEWAY;
            case 503: return HttpStatus.SERVICE_UNAVAILABLE;
            case 504: return HttpStatus.GATEWAY_TIMEOUT;
            case 599: return HttpStatus.TRUNCATED;

            default: return null;
        }
    }

    ///
    private final int code;

    ///
}
