package io.github.clamentos.gattoslab.server.http.exchange.components.resources;

import io.github.clamentos.gattoslab.security.components.AuthorizationAction;
import io.github.clamentos.gattoslab.server.http.exchange.components.MimeType;

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
