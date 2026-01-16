package io.github.clamentos.gattoslab;

///
import io.github.clamentos.gattoslab.contexts.AuthenticatedRequestContext;
import io.github.clamentos.gattoslab.contexts.StaticRequestContext;

///.
import java.util.Set;
import java.util.stream.Stream;

///.
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

///.
import org.junit.jupiter.params.provider.Arguments;

///
public class ArgumentsProvider {

    ///
    public static Stream<Arguments> staticSiteTestArgs() {

        // TODO: test broken if modified since header
        // TODO: test unsupported http methods on api paths (not just random ones)

        return Stream.of(

            Arguments.of(new StaticRequestContext(HttpMethod.GET, "/", Set.of(HttpStatus.OK), Set.of("text/html"), false)),
            Arguments.of(new StaticRequestContext(HttpMethod.GET, "/index.html", Set.of(HttpStatus.OK), Set.of("text/html"), false)),
            Arguments.of(new StaticRequestContext(HttpMethod.GET, "/this-does-not-exist.html", Set.of(HttpStatus.NOT_FOUND), Set.of("text/html"), false)),
            Arguments.of(new StaticRequestContext(HttpMethod.POST, "/index.html", Set.of(HttpStatus.METHOD_NOT_ALLOWED), Set.of("application/json"), false)),
            Arguments.of(new StaticRequestContext(HttpMethod.POST, "/this-does-not-exist.html", Set.of(HttpStatus.NOT_FOUND), Set.of("text/html"), false)),
            Arguments.of(new StaticRequestContext(HttpMethod.GET, "/", Set.of(HttpStatus.NOT_MODIFIED), Set.of(), true)),
            Arguments.of(new StaticRequestContext(HttpMethod.GET, "/index.html", Set.of(HttpStatus.NOT_MODIFIED), Set.of(), true)),
            Arguments.of(new StaticRequestContext(HttpMethod.GET, "/this-does-not-exist.html", Set.of(HttpStatus.NOT_FOUND), Set.of("text/html"), true)),
            Arguments.of(new StaticRequestContext(HttpMethod.POST, "/index.html", Set.of(HttpStatus.METHOD_NOT_ALLOWED), Set.of("application/json"), true)),
            Arguments.of(new StaticRequestContext(HttpMethod.POST, "/this-does-not-exist.html", Set.of(HttpStatus.NOT_FOUND), Set.of("text/html"), true)),
            Arguments.of(new StaticRequestContext(HttpMethod.GET, "/this-does-not-exist", Set.of(HttpStatus.NOT_FOUND), Set.of(), false)),
            Arguments.of(new StaticRequestContext(HttpMethod.POST, "/this-does-not-exist", Set.of(HttpStatus.NOT_FOUND), Set.of(), false)),
            Arguments.of(new StaticRequestContext(HttpMethod.GET, "/𝓉ℯ𝓈𝓉", Set.of(HttpStatus.NOT_FOUND), Set.of(), false)),
            Arguments.of(new StaticRequestContext(HttpMethod.GET, "/test/( ͡° ͜ʖ ͡°)", Set.of(HttpStatus.NOT_FOUND), Set.of(), false)),
            Arguments.of(new StaticRequestContext(HttpMethod.GET, "/admin/index.html", Set.of(HttpStatus.PERMANENT_REDIRECT), Set.of(), false)),
            Arguments.of(new StaticRequestContext(HttpMethod.GET, "/admin/index.html", Set.of(HttpStatus.PERMANENT_REDIRECT), Set.of(), true))
        );
    }

    ///..
    public static Stream<Arguments> adminLoginTestArgs() {

        return Stream.of(

            // TODO: more cases

            Arguments.of(new AuthenticatedRequestContext(HttpMethod.POST, "/api/session?role=ADMIN", Set.of(HttpStatus.OK), Set.of("application/json"), "test")),
            Arguments.of(new AuthenticatedRequestContext(HttpMethod.POST, "/api/session?role=ADMIN", Set.of(HttpStatus.UNAUTHORIZED), Set.of("application/json"), "wrongKey123"))
        );
    }

    ///
}
