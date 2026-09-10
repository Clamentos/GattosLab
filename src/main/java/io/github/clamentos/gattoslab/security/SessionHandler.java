package io.github.clamentos.gattoslab.security;

///
import com.sun.net.httpserver.HttpExchange;

///..
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exchange.handling.BasicHandler;
import io.github.clamentos.gattoslab.exchange.handling.ExceptionHandler;
import io.github.clamentos.gattoslab.exchange.handling.components.Api;
import io.github.clamentos.gattoslab.http.HttpHeader;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.observability.ObservabilityService;

///..
import java.util.List;
import java.util.Map;

///
public final class SessionHandler extends BasicHandler {

    ///
    private final SessionService sessionService;

    ///
    public SessionHandler(

        final ObservabilityService observabilityService,
        final ExceptionHandler exceptionHandler,
        final SessionService sessionService
    ) {

        super(observabilityService, exceptionHandler);
        this.sessionService = sessionService;
    }

    ///
    @Override
    public void doHandle(final HttpExchange exchange) {

        final Api api = (Api)exchange.getAttribute(ApplicationProperties.REQUEST_RESOURCE_ATTRIBUTE);

        if(api == null) {

            super.respondNotFound(exchange);
            return;
        }

        switch(api) {

            case LOGIN:

                if(super.rejectMethodNotAllowed(exchange, HttpMethod.POST)) return;
                final SecurityFailure securityFailure = this.sessionService.login(exchange);

                if(securityFailure == null) {

                    super.respond(exchange, HttpStatus.OK);
                }

                else {

                    super.respond(exchange, HttpStatus.UNAUTHORIZED, Map.of(), MimeType.TEXT, securityFailure.getMessage());
                    exchange.close();
                }

            return;

            case LOGOUT:

                if(super.rejectMethodNotAllowed(exchange, HttpMethod.DELETE)) return;
                final List<String> cookies = exchange.getRequestHeaders().get(HttpHeader.COOKIE.getName());

                if(cookies == null || cookies.isEmpty()) {

                    this.respond(exchange, HttpStatus.OK, ApplicationProperties.CLEAR_SITE_DATA_HEADERS);
                    exchange.close();

                    return;
                }

                this.sessionService.logout(cookies);
                super.respond(exchange, HttpStatus.OK, ApplicationProperties.CLEAR_SITE_DATA_HEADERS);
                exchange.close();

            return;

            case GET_SESSIONS:

                if(super.rejectMethodNotAllowed(exchange, HttpMethod.GET)) return;
                super.respond(exchange, HttpStatus.OK, Map.of(), MimeType.TEXT, this.sessionService.getSessions());

            return;

            default: super.respondNotFound(exchange); return;
        }
    }

    ///
}
