package io.github.clamentos.gattoslab.exchange.handling.components;

///
import io.github.clamentos.gattoslab.security.components.AuthorizationAction;

///..
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter

///
public enum Domain {

    ///
    SITE(AuthorizationAction.ALLOW),
    WWW_SITE(AuthorizationAction.ALLOW),
    EMAIL(AuthorizationAction.ALLOW),
    MTA_STS(AuthorizationAction.ALLOW),

    ///..
    VPN(AuthorizationAction.REDIRECT);

    ///
    private final AuthorizationAction authorizationAction;

    ///.
    public static Domain decode(final String value) {

        switch(value) {

            case "gattoslab.dev": return Domain.SITE;
            case "www.gattoslab.dev": return Domain.WWW_SITE;
            case "mail.gattoslab.dev": return Domain.EMAIL;
            case "mta-sts.gattoslab.dev": return Domain.MTA_STS;
            case "vpn.gattoslab.dev": return Domain.VPN;

            default: return null;
        }
    }

    ///
}
