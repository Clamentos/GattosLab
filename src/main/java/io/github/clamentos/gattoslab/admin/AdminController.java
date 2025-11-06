package io.github.clamentos.gattoslab.admin;

///
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;
import io.github.clamentos.gattoslab.utils.PropertyProvider;

///.
import jakarta.el.PropertyNotFoundException;

///.
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

///
@RestController
@RequestMapping("/admin/api/session")

///
public final class AdminController {

    ///
    private final AdminSessionService adminSessionService;

    ///..
    private final String redirectToAdminHtml;
    private final String redirectToLoginHtml;
    private final String cookieAttributes;

    ///..
    public AdminController(final AdminSessionService adminSessionService, final PropertyProvider propertyProvider)
    throws PropertyNotFoundException {

        this.adminSessionService = adminSessionService;

        final String partialRedirect =

            "<head><meta http-equiv=\"Refresh\" content=\"0; URL=" +
            propertyProvider.getProperty("app.baseUrl", String.class) +
            propertyProvider.getProperty("server.port", String.class)
        ;

        redirectToAdminHtml = partialRedirect + "/admin/index.html\"/></head>";
        redirectToLoginHtml = partialRedirect + "/admin/login.html\"/></head>";
        cookieAttributes = propertyProvider.getProperty("app.admin.cookieAttributes", String.class);
    }

    ///
    @GetMapping(path = "/login", produces = "text/html")
    public ResponseEntity<String> createSession(@RequestParam final String key, @RequestAttribute("IP_ATTRIBUTE") final String ip)
    throws ApiSecurityException {

        return ResponseEntity

            .ok()
            .header("Set-Cookie", "GattosLabSessionId=" + adminSessionService.createSession(key, ip) + ";" + cookieAttributes)
            .body(redirectToAdminHtml)
        ;
    }

    ///..
    @GetMapping(path = "/refresh")
    public ResponseEntity<Void> refreshSession(

        @CookieValue("GattosLabSessionId") final String key,
        @RequestAttribute("IP_ATTRIBUTE") final String ip

    ) throws ApiSecurityException {

        return ResponseEntity

            .ok()
            .header("Set-Cookie", "GattosLabSessionId=" + adminSessionService.refreshSession(key, ip) + ";" + cookieAttributes)
            .build()
        ;
    }

    ///..
    @GetMapping(path = "/logout", produces = "text/html")
    public ResponseEntity<String> deleteSession(

        @CookieValue("GattosLabSessionId") final String key,
        @RequestAttribute("IP_ATTRIBUTE") final String ip
    ) {

        this.adminSessionService.deleteSession(key, ip);
        return ResponseEntity.ok().body(redirectToLoginHtml);
    }

    ///
}
