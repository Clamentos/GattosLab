package io.github.clamentos.gattoslab.configuration.pojos;

///
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class SessionAdminConfig {

    ///
    private final String apiKey;
    private final int maxSessions;
    private final String cookieAttributes;

    ///
}
