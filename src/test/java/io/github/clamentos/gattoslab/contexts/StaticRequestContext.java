package io.github.clamentos.gattoslab.contexts;

///
import java.util.Set;

///.
import lombok.Getter;

///.
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

///
@Getter

///
public final class StaticRequestContext extends TestCaseRequestContext {

    ///
    private final boolean cached;

    ///
    public StaticRequestContext(

        final HttpMethod httpMethod,
        final String path,
        final Set<HttpStatus> expectedStatuses,
        final Set<String> expectedContentTypes,
        final boolean cached
    ) {

        super(httpMethod, path, expectedStatuses, expectedContentTypes);
        this.cached = cached;
    }

    ///
}
