package io.github.clamentos.gattoslab.admin;

///
import io.github.clamentos.gattoslab.configuration.PropertyProvider;
import io.github.clamentos.gattoslab.exceptions.RedirectException;
import io.github.clamentos.gattoslab.utils.GenericUtils;
import io.github.clamentos.gattoslab.web.Website;

///.
import jakarta.el.PropertyNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

///.
import java.util.HashSet;
import java.util.Set;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

///
@Component
@Slf4j

///
public final class SecurityInterceptor implements HandlerInterceptor {

    ///
    private final boolean securityEnabled;
    private final String cookieName;
    private final Set<String> authenticatedUris;

    ///..
    private final AdminSessionService adminSessionService;

    ///
    @Autowired
    public SecurityInterceptor(final PropertyProvider propertyProvider, final AdminSessionService adminSessionService, final Website website)
    throws PropertyNotFoundException {

        securityEnabled = propertyProvider.getProperty("app.security.enabled", Boolean.class);
        cookieName = propertyProvider.getProperty("app.admin.cookieName", String.class);

        authenticatedUris = new HashSet<>(website.getPaths("/admin"));
        authenticatedUris.remove("/admin/login.html");

        this.adminSessionService = adminSessionService;
    }

    ///
    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) throws RedirectException {

        if(!securityEnabled) return true;

        final Cookie[] cookies = request.getCookies();
        final String fingerprint = GenericUtils.composeFingerprint(request.getRemoteAddr(), request.getHeader("User-Agent"));

        if("/admin/login.html".equals(request.getRequestURI())) {

            if(this.check(cookies, fingerprint)) throw new RedirectException("/admin/index.html");
            return true;
        }

        if(authenticatedUris.contains(request.getRequestURI())) {

            if(this.check(cookies, fingerprint)) return true;
            throw new RedirectException("/admin/login.html");
        }

        return true;
    }

    ///.
    private boolean check(final Cookie[] cookies, final String fingerprint) {

        if(cookies == null) return false;

        for(final Cookie cookie : cookies) {

            if(cookieName.equals(cookie.getName())) {

                final boolean isOk = adminSessionService.check(cookie.getValue(), fingerprint);

                if(isOk) {

                    log.info("Admin access by fingerprint: {}", fingerprint);
                    return true;
                }

                return false;
            }
        }

        return false;
    }

    ///
}
