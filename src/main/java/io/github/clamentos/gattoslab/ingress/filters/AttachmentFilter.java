package io.github.clamentos.gattoslab.ingress.filters;

///
import io.github.clamentos.gattoslab.exceptions.ValidationException;
import io.github.clamentos.gattoslab.exceptions.handling.GlobalExceptionHandler;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpUtils;
import io.github.clamentos.gattoslab.observability.ObservabilityService;

///..
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.spec.HttpServletRequestImpl;

///..
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

///..
import java.io.IOException;

///..
import lombok.AllArgsConstructor;

///
@AllArgsConstructor

///
public final class AttachmentFilter implements Filter {

    ///
    private final ObservabilityService observabilityService;
    private final GlobalExceptionHandler globalExceptionHandler;

    ///
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {

        final HttpServerExchange exchange = ((HttpServletRequestImpl)request).getExchange();
        observabilityService.requestStarted();

        try {

            exchange.putAttachment(HttpUtils.DECODED_HTTP_METHOD, HttpMethod.decode(exchange.getRequestMethod()));
            exchange.putAttachment(HttpUtils.START_TIME_EPOCH_MS, System.currentTimeMillis());
            chain.doFilter(request, response);
        }

        catch(final ValidationException exc) {

            globalExceptionHandler.handle(exc, exchange);
        }
    }

    ///
}
