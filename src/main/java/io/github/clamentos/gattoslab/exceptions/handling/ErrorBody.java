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
    private final String requestId;

    ///
    public ErrorBody(final String title, final Throwable exception, final HttpServerExchange exchange) {

        this.title = title;
        url = exchange != null ? exchange.getRequestURL() : null;
        details = exception != null ? exception.getMessage() : null;
        requestId = exchange != null ? exchange.getRequestId() : null;
    }

    ///
}
