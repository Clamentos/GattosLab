package io.github.clamentos.gattoslab.exchange.handling.components;

///
import io.github.clamentos.gattoslab.security.components.AuthorizationAction;

///
public sealed interface Resource permits Api, StaticResource {

    ///
    public AuthorizationAction getAuthorizationAction();

    ///
}
