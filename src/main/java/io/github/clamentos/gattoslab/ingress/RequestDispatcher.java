package io.github.clamentos.gattoslab.ingress;

///
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.exceptions.IllegalHttpMethodException;
import io.github.clamentos.gattoslab.exceptions.WrappingRuntimeException;
import io.github.clamentos.gattoslab.observability.ObservabilityController;
import io.github.clamentos.gattoslab.session.SessionController;
import io.github.clamentos.gattoslab.utils.CompressingOutputStream;
import io.github.clamentos.gattoslab.utils.HttpMethod;
import io.github.clamentos.gattoslab.website.WebsiteController;

///..
import io.undertow.server.HttpServerExchange;

///..
import java.io.IOException;
import java.util.Set;

///..
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.json.JsonMapper;

///
public final class RequestDispatcher {

    ///
    private final JsonMapper jsonMapper;

    ///..
    private final SessionController sessionController;
    private final WebsiteController websiteController;
    private final ObservabilityController observabilityController;

    ///
    public RequestDispatcher(

        final JsonMapper jsonMapper,
        final SessionController sessionController,
        final WebsiteController websiteController,
        final ObservabilityController observabilityController
    ) {

        this.jsonMapper = jsonMapper;
        this.sessionController = sessionController;
        this.websiteController = websiteController;
        this.observabilityController = observabilityController;
    }

    ///
    public void dispatch(final HttpServerExchange exchange) throws ApiSecurityException, IllegalHttpMethodException, IOException, WrappingRuntimeException {

        final String requestMethod = exchange.getRequestMethod().toString();

        switch(exchange.getRequestURI()) {

            case "/api/session":

                switch(requestMethod) {

                    case "POST": sessionController.createSession(exchange); break;
                    case "PUT": sessionController.refreshSession(exchange); break;
                    case "DELETE": sessionController.deleteSession(exchange); break;

                    default: throw this.createException(requestMethod, Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE));
                }

            break;

            case "/admin/api/observability/request-metrics":

                if("POST".equals(requestMethod)) {

                    final JsonGenerator generator = this.createGenerator(exchange);

                    exchange.dispatch(() -> {

                        exchange.startBlocking();
                        observabilityController.getRequestMetrics(exchange, generator);
                    });
                }

                else throw this.createException(requestMethod, Set.of(HttpMethod.POST));

            break;

            case "/admin/api/observability/invocation-metrics":

                if("POST".equals(requestMethod)) {

                    final JsonGenerator generator = this.createGenerator(exchange);

                    exchange.dispatch(() -> {

                        exchange.startBlocking();
                        observabilityController.getInvocationMetrics(exchange, generator);
                    });
                }

                else throw this.createException(requestMethod, Set.of(HttpMethod.POST));

            break;

            case "/admin/api/observability/system-metrics":

                if("POST".equals(requestMethod)) {

                    final JsonGenerator generator = this.createGenerator(exchange);

                    exchange.dispatch(() -> {

                        exchange.startBlocking();
                        observabilityController.getSystemMetrics(exchange, generator);
                    });
                }

                else throw this.createException(requestMethod, Set.of(HttpMethod.POST));

            break;

            case "/admin/api/observability/session-metadata":

                if("GET".equals(requestMethod)) observabilityController.getSessionsMetadata(exchange);
                else throw this.createException(requestMethod, Set.of(HttpMethod.GET));

            break;

            case "/admin/api/observability/logs":

                if("POST".equals(requestMethod)) {

                    final JsonGenerator generator = this.createGenerator(exchange);

                    exchange.dispatch(() -> {

                        exchange.startBlocking();
                        observabilityController.getLogs(exchange, generator);
                    });
                }

                else throw this.createException(requestMethod, Set.of(HttpMethod.POST));

            break;

            case "/admin/api/observability/fallback-logs":

                if("GET".equals(requestMethod)) {

                    final JsonGenerator generator = this.createGenerator(exchange);

                    exchange.dispatch(() -> {

                        exchange.startBlocking();
                        this.tryWrapped(exchange, generator);
                    });
                }

                else throw this.createException(requestMethod, Set.of(HttpMethod.GET));

            break;

            default: websiteController.serveContent(exchange); break;
        }
    }

    ///.
    private JsonGenerator createGenerator(final HttpServerExchange exchange) throws IOException {

        return jsonMapper.createGenerator(new CompressingOutputStream(exchange.getOutputStream()));
    }

    ///..
    private IllegalHttpMethodException createException(final String method, final Set<HttpMethod> allowedMethods) {

        return new IllegalHttpMethodException("Method " + method + " is not supported for this endpoint. Supported methods are: " + allowedMethods);
    }

    ///..
    private void tryWrapped(final HttpServerExchange exchange, final JsonGenerator generator) throws WrappingRuntimeException {

        try { observabilityController.getFallbackLogs(exchange, generator); }
        catch(final IOException exc) { throw new WrappingRuntimeException(exc); }
    }

    ///
}
