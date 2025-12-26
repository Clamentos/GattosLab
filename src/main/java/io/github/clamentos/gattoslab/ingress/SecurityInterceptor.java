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
    public SecurityInterceptor(final PropertyProvider propertyProvider, final SessionRole roleToCheck, final SessionService sessionService)
    throws PropertyNotFoundException {

        this.roleToCheck = roleToCheck;
        cookieName = propertyProvider.getProperty("app.session.cookieName", String.class) + roleToCheck.getCookiePostfix();

        this.sessionService = sessionService;
    }

    ///
    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler)
    throws ApiSecurityException, RedirectException {

        final Cookie[] cookies = request.getCookies();
        final String fingerprint = GenericUtils.composeFingerprint(request.getRemoteAddr(), request.getHeader("User-Agent"));
        final String uri = request.getRequestURI();

        if(cookies == null) this.redirectOrFail(uri, "Cookie header was null");

        for(final Cookie cookie : cookies) {

            if(cookie != null && cookie.getName().equals(cookieName)) {

                if(sessionService.check(roleToCheck, cookie.getValue(), fingerprint)) return true;
                this.redirectOrFail(uri, "Invalid, expired or non existent session");
            }
        }

        this.redirectOrFail(uri, "No " + roleToCheck + " session cookie found in the request");
        return false;
    }

    ///.
    public void redirectOrFail(final String uri, final String message) throws ApiSecurityException, RedirectException {

        if("/admin/index.html".equals(uri) && roleToCheck == SessionRole.ADMIN) throw new RedirectException("/login.html");
        else throw new ApiSecurityException(message);
    }

    ///
}
