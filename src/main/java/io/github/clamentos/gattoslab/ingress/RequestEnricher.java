package io.github.clamentos.gattoslab.ingress;

///
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

///.
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

///..
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

///
@Component

///
public final class RequestEnricher implements HandlerInterceptor {

    ///
    @Override
    public boolean preHandle(@NonNull final HttpServletRequest request, @Nullable final HttpServletResponse response, @Nullable final Object handler) {

        request.setAttribute("IP_ATTRIBUTE", request.getRemoteAddr());
        request.setAttribute("REQUEST_METHOD_ATTRIBUTE", request.getMethod());
        request.setAttribute("START_TIME_ATTRIBUTE", System.currentTimeMillis());

        return true;
    }

    ///
}
