package io.github.clamentos.gattoslab.admin;

///
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.exceptions.RedirectException;
import io.github.clamentos.gattoslab.utils.PropertyProvider;

///.
import jakarta.el.PropertyNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
    private final AdminSessionService adminSessionService;
    
    ///..
    private final boolean securityEnabled;

    ///
    @Autowired
    public SecurityInterceptor(final AdminSessionService adminSessionService, final PropertyProvider propertyProvider)
    throws PropertyNotFoundException {

        this.adminSessionService = adminSessionService;
        securityEnabled = propertyProvider.getProperty("app.security.enabled", Boolean.class);
    }

    ///
    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler)
    throws ApiSecurityException, RedirectException {

        if(!securityEnabled) return true;

        final Cookie[] cookies = request.getCookies();
        final String ip = request.getRemoteAddr();

        if("/admin/login.html".equals(request.getRequestURI())) {

            if(this.check(cookies, ip)) throw new RedirectException("/admin/index.html");
            return true;
        }

        if(!this.check(cookies, ip)) throw new ApiSecurityException(ip);
        return true;
    }

    ///..
    private boolean check(final Cookie[] cookies, final String ip) {

        if(cookies == null || cookies.length == 0) return false;

        for(final Cookie cookie : cookies) {

            if("GattosLabSessionId".equals(cookie.getName())) {

                final boolean isOk = adminSessionService.check(cookie.getValue());

                if(isOk) {

                    log.info("Admin access by ip: {}", ip);
                    return true;
                }

                return false;
            }
        }

        return false;
    }

    ///
}
