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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

///..
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
    public SessionController(@NonNull final PropertyProvider propertyProvider, @NonNull final SessionService sessionService) throws PropertyNotFoundException {

        cookieName = propertyProvider.getProperty("app.session.cookieName", String.class);
        cookieAttributes = new EnumMap<>(SessionRole.class);

        for(final SessionRole role : SessionRole.values()) {

            final String attributes = propertyProvider.getProperty("app.session." + role.getPropertySection() + ".cookieAttributes", String.class);
            final int sessionDuration = propertyProvider.getProperty("app.session." + role.getPropertySection() + ".sessionDuration", Integer.class);

            cookieAttributes.put(role, attributes + " Max-Age=" + sessionDuration + ";");
        }

        this.sessionService = sessionService;
    }

    ///
    @PostMapping(produces = "application/json")
    public @NonNull ResponseEntity<Long> createSession(

        @RequestAttribute("IP_ATTRIBUTE") @NonNull final String ip,
        @RequestHeader(value = "Authorization", required = false) @Nullable final String key,
        @RequestHeader(value = "User-Agent", required = false) @Nullable final String userAgent,
        @RequestParam("role") @NonNull final SessionRole role

    ) throws ApiSecurityException {

        final SessionMetadata session = sessionService.createSession(key, role, ip, userAgent);

        return ResponseEntity

            .ok()
            .header("Set-Cookie", cookieName + role.name() + cookieAttributes.get(role).replace("$", session.getSessionId()))
            .body(session.getExpiresAt())
        ;
    }

    ///..
    @PutMapping(produces = "application/json")
    public @NonNull ResponseEntity<Long> refreshSession(

        @RequestAttribute("IP_ATTRIBUTE") @NonNull final String ip,
        @RequestHeader(value = "User-Agent", required = false) @Nullable final String userAgent,
        @RequestParam("role") @NonNull final SessionRole role,
        @NonNull final HttpServletRequest request

    ) throws ApiSecurityException {

        final SessionMetadata session = sessionService.refreshSession(this.getCookie(role, request), role, ip, userAgent);

        return ResponseEntity

            .ok()
            .header("Set-Cookie", cookieName + role.name() + cookieAttributes.get(role).replace("$", session.getSessionId()))
            .body(session.getExpiresAt())
        ;
    }

    ///..
    @DeleteMapping
    public @NonNull ResponseEntity<Void> deleteSession(@RequestParam("role") @NonNull final SessionRole role, @NonNull final HttpServletRequest request) {

        sessionService.deleteSession(this.getCookie(role, request));
        return ResponseEntity.ok().build();
    }

    ///..
    private @Nullable String getCookie(@NonNull final SessionRole role, @NonNull final HttpServletRequest request) {

        final Cookie[] cookies = request.getCookies();

        if(cookies != null) {

            for(final Cookie cookie : cookies) {

                if(cookie != null && cookie.getName().equals(cookieName + role.name())) {

                    return cookie.getValue();
                }
            }
        }

        return null;
    }

    ///
}
