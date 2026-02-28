package io.github.clamentos.gattoslab.exceptions.handling;

///
import io.undertow.server.HttpServerExchange;

///..
import lombok.Getter;

///
@Getter

///
public final class ErrorBody {

    ///
    private final String url;
    private final String title;
    private final String details;

    ///
    public ErrorBody(final String title, final Throwable exception, final HttpServerExchange exchange) {

        url = exchange != null ? exchange.getRequestURL() : null;
        this.title = title;
        details = exception != null ? exception.getMessage() : null;
    }

    ///
}
