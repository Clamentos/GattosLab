package io.github.clamentos.gattoslab.web;

///
import java.util.Set;

///.
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

///.
import org.springframework.http.HttpMethod;

///
@AllArgsConstructor
@Getter
@ToString(exclude = "content")

///
public final class WebsiteResource {

    ///
    private final String path;
    private final String mimeType;
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
