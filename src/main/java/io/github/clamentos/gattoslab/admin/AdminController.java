package io.github.clamentos.gattoslab.admin;

///
import io.github.clamentos.gattoslab.configuration.PropertyProvider;
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;

///.
import jakarta.el.PropertyNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

///
@RestController
@RequestMapping("/admin/api/session")

///
public final class AdminController {

    ///
    private final String cookieAttributes;
    private final String cookieName;

    ///..
    private final AdminSessionService adminSessionService;

    ///..
    @Autowired
    public AdminController(final PropertyProvider propertyProvider, final AdminSessionService adminSessionService) throws PropertyNotFoundException {

        cookieAttributes = propertyProvider.getProperty("app.admin.cookieAttributes", String.class);
        cookieName = propertyProvider.getProperty("app.admin.cookieName", String.class);

        this.adminSessionService = adminSessionService;
    }

    ///
    @PostMapping
    public ResponseEntity<Void> createSession(

        @RequestAttribute("IP_ATTRIBUTE") final String ip,
        @RequestHeader(value = "Authorization", required = false) final String key,
        @RequestHeader(value = "User-Agent", required = false) final String userAgent

    ) throws ApiSecurityException {

        final String sessionId = adminSessionService.createSession(key, ip, userAgent);
        return ResponseEntity.ok().header("Set-Cookie", cookieName + cookieAttributes.replace("$", sessionId)).build();
    }

    ///..
    @DeleteMapping
    public ResponseEntity<Void> deleteSession(final HttpServletRequest request) throws ApiSecurityException {

        final Cookie[] cookies = request.getCookies();
        if(cookies == null) throw new ApiSecurityException("Cookie header was null");

        for(final Cookie cookie : cookies) {

            if(cookie != null && cookieName.equals(cookie.getName())) {

                this.adminSessionService.deleteSession(cookie.getValue());
                return ResponseEntity.ok().build();
            }
        }

        throw new ApiSecurityException(cookieName + " was not present in the request");
    }

    ///
}
