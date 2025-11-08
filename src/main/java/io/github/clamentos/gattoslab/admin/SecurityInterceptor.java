package io.github.clamentos.gattoslab.admin;

///
import io.github.clamentos.gattoslab.exceptions.RedirectException;
import io.github.clamentos.gattoslab.utils.PropertyProvider;
import io.github.clamentos.gattoslab.web.StaticSite;

///.
import jakarta.el.PropertyNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

///.
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

///.
import lombok.extern.slf4j.Slf4j;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

///
@Component
@Slf4j

///
public final class SecurityInterceptor implements HandlerInterceptor {

    ///
    private final AdminSessionService adminSessionService;
    
    ///..
    private final Set<String> authenticatedUris;
    private final boolean securityEnabled;

    ///
    @Autowired
    public SecurityInterceptor(

        final AdminSessionService adminSessionService,
        final StaticSite staticSite,
        final PropertyProvider propertyProvider

    ) throws PropertyNotFoundException {

        this.adminSessionService = adminSessionService;

        securityEnabled = propertyProvider.getProperty("app.security.enabled", Boolean.class);
        authenticatedUris = new HashSet<>(staticSite.getSitePaths("/admin"));
    }

    ///
    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler)
    throws RedirectException {

        if(!securityEnabled) return true;

        final Cookie[] cookies = request.getCookies();
        final String ip = request.getRemoteAddr();

        if("/admin/login.html".equals(request.getRequestURI())) {

            if(this.check(cookies, ip)) throw new RedirectException("/admin/index.html");
            return true;
        }

        if(authenticatedUris.contains(request.getRequestURI())) {

            if(this.check(cookies, ip)) return true;
            throw new RedirectException("/admin/login.html");
        }

        return true;
    }

    ///.
    @EventListener
    protected void handleContextRefresh(final ContextRefreshedEvent contextRefreshedEvent) {

        final Map<RequestMappingInfo, HandlerMethod> mappings = contextRefreshedEvent

            .getApplicationContext()
            .getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class)
            .getHandlerMethods()
        ;

        for(final Map.Entry<RequestMappingInfo, HandlerMethod> entry : mappings.entrySet()) {

            authenticatedUris.addAll(entry.getKey().getDirectPaths().stream().filter(p -> p.contains("admin")).toList());
        }

        authenticatedUris.remove("/admin/login.html");
    }

    ///.
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
