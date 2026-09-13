package io.github.clamentos.gattoslab;

///
import io.github.clamentos.gattoslab.exchange.filters.components.AuthorizationAction;
import io.github.clamentos.gattoslab.exchange.handling.ResourceMappings;

///..
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Stream;

///..
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

///
class ApplicationTests {

    ///
    private static final String BASE_URI = "http://localhost:8080";
    private static final HttpClient httpClient = HttpClient.newBuilder().build();

    ///
    static {

        try {

            Application.start(new String[]{"dev"});
        }

        catch(final Exception exc) {

            System.err.println(exc);
            System.exit(1);
        }
    }

    ///
    private static HttpResponse<String> exchange(final String path) throws Exception {

        final HttpRequest request = HttpRequest.newBuilder().uri(URI.create(BASE_URI + path)).GET().build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    ///..
    private static Stream<Arguments> staticPathsProvider() {

        final ResourceMappings resourceMappings = Application.exposeMappingsForTests();

        return resourceMappings.getStaticResourcesMappings().entrySet()

            .stream()
            .map(entry -> Arguments.of(entry.getKey(), entry.getValue().getAuthorizationAction())
        );
    }

    ///..
    private static Stream<Arguments> apiPathsProvider() {

        final ResourceMappings resourceMappings = Application.exposeMappingsForTests();

        return resourceMappings.getApiMappings().entrySet()

            .stream()
            .filter(entry -> !entry.getKey().contains("login") && !entry.getKey().contains("logout"))
            .map(entry -> Arguments.of(entry.getKey())
        );
    }

    ///
    @ParameterizedTest
    @MethodSource("staticPathsProvider")
    void testStaticResources(final String path, final AuthorizationAction authorizationAction) throws Exception {

        final HttpResponse<String> response = exchange(path);
        Assertions.assertEquals(authorizationAction != AuthorizationAction.ALLOW ? 303 : 200, response.statusCode());
    }

    ///..
    @ParameterizedTest
    @MethodSource("apiPathsProvider")
    void testApisNonAuthenticated(final String path) throws Exception {

        final HttpResponse<String> response = exchange(path);
        Assertions.assertEquals(401, response.statusCode());
    }

    ///
}
