package io.github.clamentos.gattoslab.admin;

///
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.utils.PropertyProvider;

///.
import jakarta.el.PropertyNotFoundException;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
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
    private final AdminSessionService adminSessionService;

    ///..
    private final String cookieAttributes;

    ///..
    @Autowired
    public AdminController(final AdminSessionService adminSessionService, final PropertyProvider propertyProvider)
    throws PropertyNotFoundException {

        this.adminSessionService = adminSessionService;
        cookieAttributes = propertyProvider.getProperty("app.admin.cookieAttributes", String.class);
    }

    ///
    @PostMapping
    public ResponseEntity<Void> createSession(

        @RequestHeader("Authorization") final String key,
        @RequestAttribute("IP_ATTRIBUTE") final String ip
    ) {

        try {

            final String sessionId = adminSessionService.createSession(key, ip);
            return ResponseEntity.ok().header("Set-Cookie", "GattosLabSessionId=" + sessionId + ";" + cookieAttributes).build();
        }

        catch(final ApiSecurityException _) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    ///..
    @DeleteMapping
    public ResponseEntity<Void> deleteSession(

        @CookieValue("GattosLabSessionId") final String key,
        @RequestAttribute("IP_ATTRIBUTE") final String ip
    ) {

        this.adminSessionService.deleteSession(key, ip);
        return ResponseEntity.ok().build();
    }

    ///
}
