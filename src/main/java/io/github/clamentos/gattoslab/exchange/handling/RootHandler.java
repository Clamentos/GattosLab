package io.github.clamentos.gattoslab.exchange.handling;

///
import io.github.clamentos.gattoslab.exchange.handling.components.Api;
import io.github.clamentos.gattoslab.exchange.handling.components.Resource;
import io.github.clamentos.gattoslab.exchange.handling.components.StaticResource;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.http.server.HttpExchange;
import io.github.clamentos.gattoslab.observability.ObservabilityHandler;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.security.SessionHandler;
import io.github.clamentos.gattoslab.website.WebsiteHandler;

///
public final class RootHandler extends BasicHandler {

    ///
    private static final byte[] NOT_FOUND_MESSAGE_BYTES = "Not found".getBytes();

    ///.
    private final WebsiteHandler websiteHandler;
    private final SessionHandler sessionHandler;
    private final ObservabilityHandler observabilityHandler;

    ///
    public RootHandler(

        final ObservabilityService observabilityService,
        final ExceptionHandler exceptionHandler,
        final WebsiteHandler websiteHandler,
        final SessionHandler sessionHandler,
        final ObservabilityHandler observabilityHandler
    ) {

        super(observabilityService, exceptionHandler);

        this.websiteHandler = websiteHandler;
        this.sessionHandler = sessionHandler;
        this.observabilityHandler = observabilityHandler;
    }

    ///
    @Override
    public void doHandle(final HttpExchange exchange) {

        final Resource resource = exchange.getResource();

        if(resource == null) {

            super.respond(exchange, HttpStatus.NOT_FOUND, MimeType.TEXT, NOT_FOUND_MESSAGE_BYTES);
        }

        else if(resource instanceof StaticResource) {

            this.websiteHandler.handle(exchange);
        }

        else {

            switch((Api)resource) {

                case LOGIN, LOGOUT, GET_SESSIONS: this.sessionHandler.handle(exchange); break;
                case GET_LOGS, GET_REQUEST_METRICS, GET_SYSTEM_METRICS, GET_CRAWL_METRICS: this.observabilityHandler.handle(exchange); break;
            }
        }
    }

    ///
}
