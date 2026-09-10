package io.github.clamentos.gattoslab.http;

///
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter

///
public enum HttpMethod {

    ///
    GET("Allowed method: GET".getBytes()),
    HEAD("Allowed method: HEAD".getBytes()),
    OPTIONS("Allowed method: OPTIONS".getBytes()),
    POST("Allowed method: POST".getBytes()),
    PUT("Allowed method: PUT".getBytes()),
    PATCH("Allowed method: PATCH".getBytes()),
    DELETE("Allowed method: DELETE".getBytes());

    ///
    public static HttpMethod decode(final String method) {

        switch(method) {

            case "GET": return HttpMethod.GET;
            case "HEAD": return HttpMethod.HEAD;
            case "OPTIONS": return HttpMethod.OPTIONS;
            case "POST": return HttpMethod.POST;
            case "PUT": return HttpMethod.PUT;
            case "PATCH": return HttpMethod.PATCH;
            case "DELETE": return HttpMethod.DELETE;

            default: return null;
        }
    }

    ///
    private final byte[] allowedBody;

    ///
}
