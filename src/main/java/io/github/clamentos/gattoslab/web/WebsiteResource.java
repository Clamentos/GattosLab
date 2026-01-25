package io.github.clamentos.gattoslab.web;

///
import java.util.Set;

///.
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

///.
import org.springframework.http.HttpMethod;

///..
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

///
@AllArgsConstructor
@Getter
@ToString(exclude = "content")

///
public final class WebsiteResource {

    ///
    @NonNull private final String path;
    @NonNull private final String mimeType;
    @Nullable private final byte[] content;
    @NonNull private final boolean isApi;
    @NonNull private final Set<HttpMethod> supportedMethods;

    ///
    public WebsiteResource(@NonNull final String path, @NonNull final WebsiteResource from) {

        this.path = path;

        mimeType = from.getMimeType();
        content = from.getContent();
        isApi = from.isApi();
        supportedMethods = from.getSupportedMethods();
    }

    ///
}
