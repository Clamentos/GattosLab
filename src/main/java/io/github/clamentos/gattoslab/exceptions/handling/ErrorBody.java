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

        this.title = title;
        url = exchange != null ? exchange.getRequestURL() : null;
        details = exception != null ? (exception.getClass().getSimpleName() + " >> " + this.removeInternalInfo(exception.getMessage())) : null;
    }

    ///..
    private String removeInternalInfo(final String message) {

        if(message == null || message.isBlank()) return null;

        final long idx = message.indexOf("~");

        if(idx == -1) return message;
        return message.substring(message.indexOf("~"));
    }

    ///
}
