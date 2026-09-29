package io.github.clamentos.gattoslab.exchange.handling;

///
import io.github.clamentos.gattoslab.exchange.Responder;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.http.server.Handler;
import io.github.clamentos.gattoslab.http.server.HttpExchange;
import io.github.clamentos.gattoslab.observability.ObservabilityService;

///..
import java.io.IOException;

///
public abstract class BasicHandler extends Responder implements Handler {

    ///
    protected final ExceptionHandler exceptionHandler;

    ///
    protected BasicHandler(final ObservabilityService observabilityService, final ExceptionHandler exceptionHandler) {

        super(observabilityService);
        this.exceptionHandler = exceptionHandler;
    }

    ///
    @Override
    public void handle(final HttpExchange exchange) {

        try {

            this.doHandle(exchange);
        }

        catch(final IOException | RuntimeException exc) {

            this.exceptionHandler.handleUncaught(exchange, exc);
        }
    }

    ///.
    protected abstract void doHandle(final HttpExchange exchange) throws IOException;

    ///..
    protected boolean rejectMethodNotAllowed(final HttpExchange exchange, final HttpMethod method) {

        if(exchange.getMethod() != method) {

            super.respond(exchange, HttpStatus.METHOD_NOT_ALLOWED, MimeType.TEXT, method.getAllowedBody());
            exchange.close();

            return true;
        }

        return false;
    }

    ///
}
