package io.github.clamentos.gattoslab.server.http.exchange.components.resources;

///
import io.github.clamentos.gattoslab.security.components.AuthorizationAction;

///
public sealed interface Resource permits Api, StaticResource {

    ///
    public AuthorizationAction getAuthorizationAction();

    ///
}
