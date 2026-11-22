package io.github.clamentos.gattoslab.server.http.exchange.components;

///
import java.nio.charset.StandardCharsets;

///..
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter

///
public enum HttpMethod {

    ///
    GET("Allowed method: GET".getBytes(StandardCharsets.ISO_8859_1)),
    QUERY("Allowed method: QUERY".getBytes(StandardCharsets.ISO_8859_1)),
    HEAD("Allowed method: HEAD".getBytes(StandardCharsets.ISO_8859_1)),
    OPTIONS("Allowed method: OPTIONS".getBytes(StandardCharsets.ISO_8859_1)),
    POST("Allowed method: POST".getBytes(StandardCharsets.ISO_8859_1)),
    DELETE("Allowed method: DELETE".getBytes(StandardCharsets.ISO_8859_1));

    ///
    private final byte[] message;

    ///
    public static HttpMethod decode(final String method) {

        switch(method) {

            case "GET": return HttpMethod.GET;
            case "QUERY": return HttpMethod.QUERY;
            case "HEAD": return HttpMethod.HEAD;
            case "OPTIONS": return HttpMethod.OPTIONS;
            case "POST": return HttpMethod.POST;
            case "DELETE": return HttpMethod.DELETE;

            default: return null;
        }
    }

    ///
}
