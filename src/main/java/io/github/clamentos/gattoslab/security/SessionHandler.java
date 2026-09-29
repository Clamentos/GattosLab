package io.github.clamentos.gattoslab.security;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.Pair;
import io.github.clamentos.gattoslab.exchange.handling.BasicHandler;
import io.github.clamentos.gattoslab.exchange.handling.ExceptionHandler;
import io.github.clamentos.gattoslab.exchange.handling.components.Api;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.http.server.HttpExchange;
import io.github.clamentos.gattoslab.observability.ObservabilityService;

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

        final Api api = (Api)exchange.getResource();

        switch(api) {

            case LOGIN:

                if(super.rejectMethodNotAllowed(exchange, HttpMethod.POST)) return;

                final Pair<SecurityFailure, Long> loginResult = this.sessionService.login(exchange);
                final SecurityFailure securityFailure = loginResult.getA();

                if(securityFailure == null) {

                    super.respond(exchange, HttpStatus.OK, MimeType.TEXT, Long.toString(loginResult.getB()).getBytes());
                }

                else {

                    super.respond(exchange, HttpStatus.UNAUTHORIZED, MimeType.TEXT, securityFailure.getMessage());
                    exchange.close();
                }

            return;

            case LOGOUT:

                if(super.rejectMethodNotAllowed(exchange, HttpMethod.DELETE)) return;

                this.sessionService.logout(exchange);
                super.respond(exchange, HttpStatus.OK, ApplicationProperties.CLEAR_SITE_DATA_HEADERS);
                exchange.close();

            return;

            case GET_SESSIONS:

                if(super.rejectMethodNotAllowed(exchange, HttpMethod.GET)) return;
                super.respond(exchange, HttpStatus.OK, MimeType.TEXT, this.sessionService.getSessions());

            return;

            default: super.respond(exchange, HttpStatus.NOT_FOUND); return;
        }
    }

    ///
}
