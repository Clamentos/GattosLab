package io.github.clamentos.gattoslab;

///
import java.util.Map;
import java.util.stream.Stream;

///.
import org.springframework.http.HttpMethod;

///.
import org.junit.jupiter.params.provider.Arguments;

///
public class ArgumentsProvider {

    ///
    public static Stream<Arguments> staticSiteTestArgs() {

        return Stream.of(

            Arguments.of(Map.of("method", HttpMethod.GET, "path", "/", "cache", false, "status", 200)),
            Arguments.of(Map.of("method", HttpMethod.GET, "path", "/index.html", "cache", false, "status", 200)),
            Arguments.of(Map.of("method", HttpMethod.GET, "path", "/this-does-not-exist.html", "cache", false, "status", 404)),
            Arguments.of(Map.of("method", HttpMethod.POST, "path", "/index.html", "cache", false, "status", 405)),

            Arguments.of(Map.of("method", HttpMethod.GET, "path", "/", "cache", true, "status", 304)),
            Arguments.of(Map.of("method", HttpMethod.GET, "path", "/index.html", "cache", true, "status", 304)),
            Arguments.of(Map.of("method", HttpMethod.GET, "path", "/this-does-not-exist.html", "cache", true, "status", 404)),
            Arguments.of(Map.of("method", HttpMethod.POST, "path", "/index.html", "cache", true, "status", 405))
        );
    }

    ///..
    public static Stream<Arguments> adminLoginTestArgs() {

        return Stream.of(

            Arguments.of(Map.of("method", HttpMethod.POST, "path", "/admin/api/session", "apiKey", "test", "status", 200)),
            Arguments.of(Map.of("method", HttpMethod.POST, "path", "/admin/api/session", "apiKey", "wrongKey123", "status", 401))
        );
    }

    ///
}
