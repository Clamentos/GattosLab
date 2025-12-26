package io.github.clamentos.gattoslab.session;

///
import io.github.clamentos.gattoslab.configuration.PropertyProvider;
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;

///.
import jakarta.el.PropertyNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

///.
import java.util.EnumMap;
import java.util.Map;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

///
@RestController
@RequestMapping("/api/session")

///
public final class SessionController {

    ///
    private final String cookieName;
    private final Map<SessionRole, String> cookieAttributes;

    ///..
    private final SessionService sessionService;

    ///..
    @Autowired
    public SessionController(final PropertyProvider propertyProvider, final SessionService sessionService) throws PropertyNotFoundException {

        cookieName = propertyProvider.getProperty("app.session.cookieName", String.class);
        cookieAttributes = new EnumMap<>(SessionRole.class);

        for(final SessionRole role : SessionRole.values()) {

            cookieAttributes.put(

                role,
                propertyProvider.getProperty("app.session." + role.getPropertySection() + ".cookieAttributes", String.class)
            );
        }

        this.sessionService = sessionService;
    }

    ///
    @PostMapping
    public ResponseEntity<Void> createSession(

        @RequestAttribute("IP_ATTRIBUTE") final String ip,
        @RequestHeader(value = "Authorization", required = false) final String key,
        @RequestHeader(value = "User-Agent", required = false) final String userAgent,
        @RequestParam("role") final SessionRole role

    ) throws ApiSecurityException {

        final String sessionId = sessionService.createSession(key, role, ip, userAgent);

        return ResponseEntity

            .ok()
            .header("Set-Cookie", cookieName + role.getCookiePostfix() + cookieAttributes.get(role).replace("$", sessionId))
            .build()
        ;
    }

    ///..
    @DeleteMapping
    public ResponseEntity<Void> deleteSession(@RequestParam("role") final SessionRole role, final HttpServletRequest request) {

        final Cookie[] cookies = request.getCookies();

        if(cookies != null) {

            for(final Cookie cookie : cookies) {

                if(cookie != null && cookie.getName().equals(cookieName + role)) {

                    this.sessionService.deleteSession(cookie.getValue());
                    break;
                }
            }
        }

        return ResponseEntity.ok().build();
    }

    ///
}
