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
    OK(200, "HTTP/1.1 200 OK\r\n".getBytes()),
    NO_CONTENT(204, "HTTP/1.1 204 No Content\r\n".getBytes()),
    SEE_OTHER(303, "HTTP/1.1 303 See Other\r\n".getBytes()),
    BAD_REQUEST(400, "HTTP/1.1 400 Bad Request\r\n".getBytes()),
    UNAUTHORIZED(401, "HTTP/1.1 401 Unauthorized\r\n".getBytes()),
    FORBIDDEN(403, "HTTP/1.1 403 Forbidden\r\n".getBytes()),
    NOT_FOUND(404, "HTTP/1.1 404 Not Found\r\n".getBytes()),
    METHOD_NOT_ALLOWED(405, "HTTP/1.1 405 Method Not Allowed\r\n".getBytes()),
    TOO_MANY_REQUESTS(429, "HTTP/1.1 429 Too Many Requests\r\n".getBytes()),
    INTERNAL_SERVER_ERROR(500, "HTTP/1.1 500 Internal Server Error\r\n".getBytes()),
    SERVICE_UNAVAILABLE(503, "HTTP/1.1 503 Service Unavailable\r\n".getBytes()),

    ///..
    TRUNCATED(599, "HTTP/1.1 599 Truncated\r\n".getBytes());

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
            case 503: return HttpStatus.SERVICE_UNAVAILABLE;
            case 599: return HttpStatus.TRUNCATED;

            default: return null;
        }
    }

    ///
    private final int code;
    private final byte[] valueForResponse;

    ///
}
