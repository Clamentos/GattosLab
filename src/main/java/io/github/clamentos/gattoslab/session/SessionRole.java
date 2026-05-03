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
    private static final String SOURCE_DECODE = "SessionRole.decode";
    private final String propertySection;

    ///
    public static SessionRole decode(final HttpServerExchange exchange) throws ApiSecurityException {

        final Deque<String> values = exchange.getQueryParameters().get("role");

        if(values != null) {

            final String value = values.peekFirst();

            if(value != null) {

                if("ADMIN".equals(value)) return SessionRole.ADMIN;
                throw new ApiSecurityException("Unknown role '" + value + "'", SOURCE_DECODE);
            }
        }

        throw new ApiSecurityException("Query param cannot be null", SOURCE_DECODE);
    }

    ///
}
