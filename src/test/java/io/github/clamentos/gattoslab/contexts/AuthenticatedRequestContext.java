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
public final class AuthenticatedRequestContext extends TestCaseRequestContext {

    ///
    private final String apiKey;

    ///
    public AuthenticatedRequestContext(

        final HttpMethod httpMethod,
        final String path,
        final Set<HttpStatus> expectedStatuses,
        final Set<String> expectedContentTypes,
        final String apiKey
    ) {

        super(httpMethod, path, expectedStatuses, expectedContentTypes);
        this.apiKey = apiKey;
    }

    ///
}
