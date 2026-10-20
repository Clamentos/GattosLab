package io.github.clamentos.gattoslab.security.components;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.datastructures.MutableString;
import io.github.clamentos.gattoslab.datastructures.StreamWriter;
import io.github.clamentos.gattoslab.datastructures.interfaces.Streamable;

///..
import java.io.IOException;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class Session implements Streamable {

    ///
    private final MutableString sessionId;
    private final String fingerprint;
    private final long expiresAt;

    ///
    @Override
    public void stream(final StreamWriter writer) throws IOException {

        writer.write(String.valueOf(this.fingerprint));
        writer.write(ApplicationProperties.FIELD_SEPARATOR);
        writer.write(Long.toString(this.expiresAt));
    }

    ///
}
