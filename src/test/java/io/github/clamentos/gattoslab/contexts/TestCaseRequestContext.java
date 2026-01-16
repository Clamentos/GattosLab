package io.github.clamentos.gattoslab.contexts;

///
import java.util.Set;

///.
import lombok.AllArgsConstructor;
import lombok.Getter;

///.
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

///
@AllArgsConstructor
@Getter

///
public class TestCaseRequestContext {

    ///
    private final HttpMethod httpMethod;
    private final String path;
    private final Set<HttpStatus> expectedStatuses;
    private final Set<String> expectedContentTypes;

    ///
}
