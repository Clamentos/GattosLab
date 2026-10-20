package io.github.clamentos.gattoslab.exchange.handling.components;

///
import io.github.clamentos.gattoslab.http.MimeType;
import io.github.clamentos.gattoslab.security.components.AuthorizationAction;

///..
import java.nio.file.Path;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;

///
@AllArgsConstructor
@Getter

///
public final class StaticResource implements Resource {

    ///
    private final AuthorizationAction authorizationAction;
    private final MimeType mimeType;
    private final boolean isCacheable;
    private final Path diskPath;
    private final byte[] compressedContent;

    ///
}
