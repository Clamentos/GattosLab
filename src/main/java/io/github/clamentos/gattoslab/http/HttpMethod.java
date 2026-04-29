package io.github.clamentos.gattoslab.http;

///
import io.github.clamentos.gattoslab.exceptions.ValidationException;

///..
import io.undertow.util.HttpString;

///..
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)

///
public enum HttpMethod {

    ///
    GET(new HttpString("GET")),
    POST(new HttpString("POST")),
    PUT(new HttpString("PUT")),
    PATCH(new HttpString("PATCH")),
    DELETE(new HttpString("DELETE")),
    OPTIONS(new HttpString("OPTIONS"));

    ///
    private final HttpString method;

    ///
    public static HttpMethod decode(final HttpString method) throws ValidationException {

        if(HttpMethod.GET.method.equals(method)) return HttpMethod.GET;
        if(HttpMethod.POST.method.equals(method)) return HttpMethod.POST;
        if(HttpMethod.PUT.method.equals(method)) return HttpMethod.PUT;
        if(HttpMethod.PATCH.method.equals(method)) return HttpMethod.PATCH;
        if(HttpMethod.DELETE.method.equals(method)) return HttpMethod.DELETE;
        if(HttpMethod.OPTIONS.method.equals(method)) return HttpMethod.OPTIONS;

        throw new ValidationException("HttpMethod.decode~Unknown method " + method);
    }

    ///
}
