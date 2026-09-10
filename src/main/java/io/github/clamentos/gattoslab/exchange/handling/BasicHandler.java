package io.github.clamentos.gattoslab.exchange.handling;

///
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.FastAsciiJoiner;
import io.github.clamentos.gattoslab.exchange.Responder;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.observability.ObservabilityService;

///..
import java.io.IOException;
import java.util.Map;
import java.util.Set;

///
public abstract class BasicHandler extends Responder implements HttpHandler {

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

        catch(final IOException | InterruptedException | RuntimeException exc) {

            if(exc instanceof InterruptedException) Thread.currentThread().interrupt();
            this.exceptionHandler.handleUnexpected(exchange, exc);
        }
    }

    ///.
    protected abstract void doHandle(final HttpExchange exchange) throws IOException, InterruptedException;

    ///..
    protected boolean rejectMethodNotAllowed(final HttpExchange exchange, final HttpMethod method) {

        if(exchange.getAttribute(ApplicationProperties.REQUEST_METHOD_ATTRIBUTE) != method) {

            super.respond(exchange, HttpStatus.METHOD_NOT_ALLOWED, Map.of(), MimeType.TEXT, method.getAllowedBody());
            exchange.close();

            return true;
        }

        return false;
    }

    ///..
    protected boolean rejectMethodNotAllowed(final HttpExchange exchange, final Set<HttpMethod> methods) {

        if(!methods.contains(exchange.getAttribute(ApplicationProperties.REQUEST_METHOD_ATTRIBUTE))) {

            final FastAsciiJoiner joiner = new FastAsciiJoiner(methods.size());

            for(final HttpMethod method : methods) {

                joiner.add(method.name());
            }

            super.respond(exchange, HttpStatus.METHOD_NOT_ALLOWED, Map.of(), MimeType.TEXT, joiner.toByteArray());
            exchange.close();

            return true;
        }

        return false;
    }

    ///..
    protected void respondNotFound(final HttpExchange exchange) {

        super.respond(exchange, HttpStatus.NOT_FOUND, Map.of(), null, (byte[])null);
    }

    ///
}
