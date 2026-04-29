package io.github.clamentos.gattoslab.website;

///
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.MimeType;

///..
import java.util.Set;

///..
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

///
@AllArgsConstructor
@Getter
@ToString(exclude = "content")

///
public final class WebsiteResource {

    ///
    private final String path;
    private final MimeType mimeType;
    private final byte[] content;
    private final Set<HttpMethod> supportedMethods;
    private final boolean cacheable;

    ///
    public WebsiteResource(final String path, final WebsiteResource from) {

        this.path = path;

        mimeType = from.getMimeType();
        content = from.getContent();
        supportedMethods = from.getSupportedMethods();
        cacheable = from.isCacheable();
    }

    ///
}
