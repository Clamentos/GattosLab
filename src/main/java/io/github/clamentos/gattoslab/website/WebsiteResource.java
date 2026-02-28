package io.github.clamentos.gattoslab.website;

///
import io.github.clamentos.gattoslab.utils.HttpMethod;
import io.github.clamentos.gattoslab.utils.MimeType;

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
    private final boolean isApi;
    private final Set<HttpMethod> supportedMethods;

    ///
    public WebsiteResource(final String path, final WebsiteResource from) {

        this.path = path;

        mimeType = from.getMimeType();
        content = from.getContent();
        isApi = from.isApi();
        supportedMethods = from.getSupportedMethods();
    }

    ///
}
