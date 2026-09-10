package io.github.clamentos.gattoslab.security;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.FastAsciiJoiner;
import io.github.clamentos.gattoslab.observability.Printable;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class Session implements Printable {

    ///
    private final String sessionId;
    private final String fingerprint;
    private final long expiresAt;

    ///
    @Override
    public void appendBytes(final FastAsciiJoiner joiner) {

        joiner.add(this.fingerprint);
        joiner.add(ApplicationProperties.FIELD_SEPARATOR_STRING);
        joiner.add(Long.toString(this.expiresAt));
    }

    ///
}
