package io.github.clamentos.gattoslab.ingress;

///
import io.github.clamentos.gattoslab.configuration.PropertyProvider;
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.exceptions.RedirectException;
import io.github.clamentos.gattoslab.session.SessionRole;
import io.github.clamentos.gattoslab.session.SessionService;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///.
import jakarta.el.PropertyNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.web.servlet.HandlerInterceptor;

///..
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

///
@Slf4j

///
public final class SecurityInterceptor implements HandlerInterceptor {

    ///
    private final String cookieName;
    private final SessionRole roleToCheck;

    ///..
    private final SessionService sessionService;

    ///
    public SecurityInterceptor(

        @NonNull final PropertyProvider propertyProvider,
        @NonNull final SessionRole roleToCheck,
        @NonNull final SessionService sessionService

    ) throws PropertyNotFoundException {

        this.roleToCheck = roleToCheck;
        cookieName = propertyProvider.getProperty("app.session.cookieName", String.class) + roleToCheck.name();

        this.sessionService = sessionService;
    }

    ///
    @Override
    public boolean preHandle(@NonNull final HttpServletRequest request, @Nullable final HttpServletResponse response, @Nullable final Object handler)
    throws Exception {

        final Cookie[] cookies = request.getCookies();
        final String fingerprint = GenericUtils.composeFingerprint(request.getRemoteAddr(), request.getHeader("User-Agent"));
        final String uri = request.getRequestURI();

        if(cookies == null) throw this.redirectOrFail(uri, "Cookie header was null");

        for(final Cookie cookie : cookies) {

            if(cookie != null && cookieName.equals(cookie.getName())) {

                if(sessionService.check(roleToCheck, cookie.getValue(), fingerprint) != null) return true;
                throw this.redirectOrFail(uri, "Invalid, expired or non existent session");
            }
        }

        throw this.redirectOrFail(uri, "No " + roleToCheck + " session cookie found in the request");
    }

    ///.
    public @NonNull Exception redirectOrFail(@Nullable final String uri, @NonNull final String message) {

        if(uri != null && uri.endsWith(".html") && roleToCheck == SessionRole.ADMIN) return new RedirectException("/login.html");
        else return new ApiSecurityException(message);
    }

    ///
}
