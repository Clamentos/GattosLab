package io.github.clamentos.gattoslab.session;

///
import io.github.clamentos.gattoslab.exceptions.ApiSecurityException;

///..
import io.undertow.server.HttpServerExchange;

///..
import java.util.Deque;

///..
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter

///
public enum SessionRole {

    ///
    ADMIN("admin");

    ///
    private final String propertySection;

    ///
    public static SessionRole fromParam(final HttpServerExchange exchange) throws ApiSecurityException {

        final Deque<String> values = exchange.getQueryParameters().get("role");
        if(values == null) throw new ApiSecurityException("Query param cannot be null");

        final String value = values.peekFirst();
        if(value == null) throw new ApiSecurityException("Query param cannot be null");

        try { return SessionRole.valueOf(value); }
        catch(final IllegalArgumentException _) { throw new ApiSecurityException("Unknown role: " + value); }
    }

    ///
}
