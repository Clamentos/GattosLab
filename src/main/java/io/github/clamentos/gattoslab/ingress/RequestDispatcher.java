package io.github.clamentos.gattoslab.ingress;

///
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.exceptions.IllegalHttpMethodException;
import io.github.clamentos.gattoslab.exceptions.handling.GlobalExceptionHandler;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpUtils;
import io.github.clamentos.gattoslab.observability.ObservabilityController;
import io.github.clamentos.gattoslab.observability.ObservabilityService;
import io.github.clamentos.gattoslab.session.SessionController;
import io.github.clamentos.gattoslab.utils.CompressingOutputStream;
import io.github.clamentos.gattoslab.utils.ExceptionalConsumer;
import io.github.clamentos.gattoslab.website.Apis;
import io.github.clamentos.gattoslab.website.Website;
import io.github.clamentos.gattoslab.website.WebsiteController;

///..
import io.undertow.server.HttpServerExchange;

///..
import java.util.Set;

///..
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.json.JsonMapper;

///
public final class RequestDispatcher {

    ///
    private final JsonMapper jsonMapper;
    private final GlobalExceptionHandler exceptionHandler;

    ///..
    private final Website website;
    private final SessionController sessionController;
    private final WebsiteController websiteController;
    private final ObservabilityController observabilityController;

    ///..
    private final ObservabilityService observabilityService;

    ///
    public RequestDispatcher(

        final JsonMapper jsonMapper,
        final GlobalExceptionHandler exceptionHandler,
        final Website website,
        final SessionController sessionController,
        final WebsiteController websiteController,
        final ObservabilityController observabilityController,
        final ObservabilityService observabilityService
    ) {

        this.jsonMapper = jsonMapper;
        this.exceptionHandler = exceptionHandler;

        this.website = website;
        this.sessionController = sessionController;
        this.websiteController = websiteController;
        this.observabilityController = observabilityController;

        this.observabilityService = observabilityService;
    }

    ///
    public boolean dispatch(final HttpServerExchange exchange) throws ApiSecurityException, IllegalHttpMethodException, JacksonException {

        final HttpMethod requestMethod = exchange.getAttachment(HttpUtils.DECODED_HTTP_METHOD);

        switch(exchange.getRequestURI()) {

            case Apis.AUTH_ENDPOINT:

                switch(requestMethod) {

                    case POST: sessionController.createSession(exchange); break;
                    case PUT: sessionController.refreshSession(exchange); break;
                    case DELETE: sessionController.deleteSession(exchange); break;

                    default: throw this.rejectInvalidHttpMethod(requestMethod, website.getContent(Apis.AUTH_ENDPOINT).getSupportedMethods());
                }

            return true;

            case Apis.REQUEST_METRICS_ENDPOINT:

                if(requestMethod == HttpMethod.POST) this.doDispatch(observabilityController::getRequestMetrics, exchange);
                else throw this.rejectInvalidHttpMethod(requestMethod, website.getContent(Apis.REQUEST_METRICS_ENDPOINT).getSupportedMethods());

            return false;

            case Apis.INVOCATION_METRICS_ENDPOINT:

                if(requestMethod == HttpMethod.POST) this.doDispatch(observabilityController::getInvocationMetrics, exchange);
                else throw this.rejectInvalidHttpMethod(requestMethod, website.getContent(Apis.INVOCATION_METRICS_ENDPOINT).getSupportedMethods());

            return false;

            case Apis.SYSTEM_METRICS_ENDPOINT:

                if(requestMethod == HttpMethod.POST) this.doDispatch(observabilityController::getSystemMetrics, exchange);
                else throw this.rejectInvalidHttpMethod(requestMethod, website.getContent(Apis.SYSTEM_METRICS_ENDPOINT).getSupportedMethods());

            return false;

            case Apis.SESSION_METADATA_ENDPOINT:

                if(requestMethod == HttpMethod.GET) observabilityController.getSessionsMetadata(exchange);
                else throw this.rejectInvalidHttpMethod(requestMethod, website.getContent(Apis.SESSION_METADATA_ENDPOINT).getSupportedMethods());

            return true;

            case Apis.LOGS_ENDPOINT:

                if(requestMethod == HttpMethod.POST) this.doDispatch(observabilityController::getLogs, exchange);
                else throw this.rejectInvalidHttpMethod(requestMethod, website.getContent(Apis.LOGS_ENDPOINT).getSupportedMethods());

            return false;

            case Apis.FALLBACK_LOGS_ENDPOINT:

                if(requestMethod == HttpMethod.GET) this.doDispatch(observabilityController::getFallbackLogs, exchange);
                else throw this.rejectInvalidHttpMethod(requestMethod, website.getContent(Apis.FALLBACK_LOGS_ENDPOINT).getSupportedMethods());

            return false;

            default:

                websiteController.serveContent(exchange);

            return true;
        }
    }

    ///.
    // NOTE: Try with resources breaks this implementation:
    // When an exception is triggered, "generator" is immediately closed before the catch, thus closing the underlying undertow stream.
    // This makes it impossible to utilize the stream for sending an error response because the exchange will be in the "response already sent" state
    // even if no bytes were actually sent.

    @SuppressWarnings("squid:S2093")
    private void doDispatch(final ExceptionalConsumer<HttpServerExchange, JsonGenerator> serviceMethod, final HttpServerExchange exchange) {

        exchange.dispatch(() -> {

            boolean skipUpdateMetrics = false;
            boolean skipClose = false;
            JsonGenerator generator = null;

            exchange.startBlocking();

            try {

                generator = jsonMapper.createGenerator(new CompressingOutputStream(exchange.getOutputStream()));
                serviceMethod.execute(exchange, generator);
                generator.flush();
                generator.close();
            }

            catch(final Exception exc) {

                skipUpdateMetrics = exceptionHandler.handleWithReset(exc, exchange);
                skipClose = true;
            }

            finally {

                if(generator != null && !skipClose) {

                    generator.flush();
                    generator.close();
                }

                if(!skipUpdateMetrics) observabilityService.updateRequestMetrics(exchange);
            }
        });
    }

    ///..
    private IllegalHttpMethodException rejectInvalidHttpMethod(final HttpMethod method, final Set<HttpMethod> allowedMethods) {

        return new IllegalHttpMethodException(

            "Method '" + method.name() + "' is not supported for this endpoint. Supported methods are: " + allowedMethods,
            "RequestDispatcher.rejectInvalidHttpMethod"
        );
    }

    ///
}
