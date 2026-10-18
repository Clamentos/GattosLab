package io.github.clamentos.gattoslab.exchange.handling.components;

///
import io.github.clamentos.gattoslab.exchange.filters.components.AuthorizationAction;
import io.github.clamentos.gattoslab.http.MimeType;

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
