package io.github.clamentos.gattoslab.http;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class HttpHeader {

    ///
    private final HttpHeaderName name;
    private final String value;

    ///
}
