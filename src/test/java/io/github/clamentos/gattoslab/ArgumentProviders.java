package io.github.clamentos.gattoslab;

///
import io.github.clamentos.gattoslab.exchange.handling.components.Api;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.util.stream.Stream;

///..
import org.junit.jupiter.params.provider.Arguments;

///
public class ArgumentProviders {

    ///
    public static final Thread initializer = GenericUtils.spawnVirtualThread("test-initializer", () -> {

        try {

            System.out.println("Make sure that observability data is present, otherwise some tests might fail");
            Application.main(new String[]{"test"});
        }

        catch(final Exception exc) {

            System.err.println(exc);
            System.exit(1);
        }
    });

    ///.
    private static void waitForInit() {

        while(Application.exposeMappingsForTests() == null || Application.exposePropertiesForTests() == null) {

            GenericUtils.silentSleep(200);
        }
    }

    ///.
    public static Stream<Arguments> staticPathsProvider() {

        waitForInit();

        return Application.exposeMappingsForTests()

            .getStaticResourcesMappings()
            .entrySet()
            .stream()
            .map(entry -> Arguments.of(entry.getKey(), entry.getValue().getAuthorizationAction())
        );
    }

    ///..
    public static Stream<Arguments> apiPathsProvider() {

        waitForInit();

        return Application.exposeMappingsForTests().getApiMappings().entrySet()

            .stream()
            .filter(entry -> !entry.getKey().contains("login") && !entry.getKey().contains("logout"))
            .map(entry -> Arguments.of(entry.getKey())
        );
    }

    ///..
    public static Stream<Arguments> weirdPathProvider() {

        waitForInit();

        return Stream.of(

            Arguments.of("/\u00e9.html"),
            Arguments.of("/\u0444\u0430\u0439\u043b.html"),
            Arguments.of("/\u65e5\u672c\u8a9e.html"),
            Arguments.of("/\ud83c\udf89.html"),
            Arguments.of("/\u0645\u0631\u062d\u0628\u0627.html"),
            Arguments.of("/index\u00e9.html"),
            Arguments.of("/../../etc/passwd"),
            Arguments.of("/..%2f..%2f..%2fetc%2fpasswd"),
            Arguments.of("/..%252f..%252f..%252fetc%252fpasswd"),
            Arguments.of("/index.html%00.txt"),
            Arguments.of("/admin/../index.html"),
            Arguments.of("/admin/..%2f..%2findex.html"),
            Arguments.of("/Admin/index.html"),
            Arguments.of("/admin/"),
            Arguments.of("//admin/index.html"),
            Arguments.of("/./admin/index.html"),
            Arguments.of("/api/authentication/sessions/"),
            Arguments.of("/a\u0301min/index.html"),
            Arguments.of("/api/Authentication/Sessions"),
            Arguments.of("/index\t.html"),
            Arguments.of("/index\u2028.html"),
            Arguments.of("/in\u200bdex.html"),
            Arguments.of("http://localhost:8080/index.html"),
            Arguments.of("localhost:8080"),
            Arguments.of("/../../src/main/java/io/github/clamentos/gattoslab/Application.java"),
            Arguments.of("/../../src/main/resources/application-prod.properties"),
            Arguments.of("/admin%2Findex.html"),
            Arguments.of("/admin\\..\\..\\etc\\passwd"),
            Arguments.of("/%252e%252e%252f%252e%252e%252fetc%252fpasswd")
        );
    }

    ///..
    public static Stream<Arguments> hugeLineProvider() {

        waitForInit();

        return Stream.of(

            Arguments.of(1000, HttpStatus.NOT_FOUND),
            Arguments.of(300000, HttpStatus.CONTENT_TOO_LARGE)
        );
    }

    ///..
    public static Stream<Arguments> hugeHeaderProvider() {

        waitForInit();

        return Stream.of(

            Arguments.of(1000, HttpStatus.OK),
            Arguments.of(300000, HttpStatus.CONTENT_TOO_LARGE)
        );
    }

    ///..
    public static Stream<Arguments> weirdRequestProvider() {

        waitForInit();

        return Stream.of(

            Arguments.of("/index.html\r\n\r\n", HttpStatus.BAD_REQUEST),
            Arguments.of("GET /index.html\r\n", HttpStatus.BAD_REQUEST),
            Arguments.of("GET /index.html\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.BAD_REQUEST),
            Arguments.of("GET /index.html HTTP/1.1 EXTRA PARTS\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.BAD_REQUEST),
            Arguments.of("GET /index.html HTTP/9.9\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.BAD_REQUEST),
            Arguments.of("GET /index.html HTTP/1.1\r\nUser-Agent junit-tests\r\n\r\n", HttpStatus.BAD_REQUEST),
            Arguments.of("GET /index.html HTTP/1.1\r\nUser-Agent: junit\u0000tests\r\n\r\n", HttpStatus.OK),
            Arguments.of("GET /index.html HTTP/1.1\r\nHost: localhost\r\nContent-Length: 100\r\n\r\n", HttpStatus.OK),
            Arguments.of("GET /index.html HTTP/1.1\r\nHost: localhost\r\nTransfer-Encoding: chunked\r\n\r\n", HttpStatus.OK),
            Arguments.of("GET /index.html HTTP/1.1\r\nHost: localhost\r\nContent-Length: 5\r\nTransfer-Encoding: chunked\r\n\r\n", HttpStatus.OK),
            Arguments.of("GET /index.html HTTP/1.1\r\nHost: localhost\r\nHost: evil.com\r\nHost: another.com\r\n\r\n", HttpStatus.OK),

            Arguments.of("GET /index.html HTTP/1.1\r\nHost: localhost\r\nX-Test: \u00e9\u00e8\u00ea\u0444\u0430\u0439\u043b\u65e5\u672c\u8a9e\r\n\r\n", HttpStatus.OK),

            Arguments.of("GET /\u0000i\u0000n\u0000d\u0000e\u0000x\u0000.\u0000h\u0000t\u0000m\u0000l HTTP/1.1\r\nHost: localhost\r\n\r\n", HttpStatus.NOT_FOUND),

            Arguments.of("GET /index.html HTTP/1.1\r\nHost: localhost\r\nX-Empty:\r\n\r\n", HttpStatus.OK),
            Arguments.of("GET /index.html HTTP/1.1\r\nHost: localhost\r\nX-Test:    \r\n\r\n", HttpStatus.OK),
            Arguments.of("\r\n", HttpStatus.BAD_REQUEST),

            Arguments.of("GET /api/authentication/sessions HTTP/1.1\r\nCookie: GattosLabSessionId=forged-session-id\r\n\r\n", HttpStatus.UNAUTHORIZED),

            Arguments.of("GET /api/authentication/sessions HTTP/1.1\r\nCookie: GattosLabSessionId=forged-session-id; GattosLabSessionId=forged-session-id2\r\n\r\n", HttpStatus.UNAUTHORIZED),

            Arguments.of("GET /api/authentication/sessions HTTP/1.1\r\nHost: localhost\r\nAuthorization: Bearer fake-token\r\n\r\n", HttpStatus.UNAUTHORIZED),

            Arguments.of("GET /api/authentication/sessions HTTP/1.1\r\nHost: localhost\r\nX-Forwarded-For: 127.0.0.1\r\nX-Real-IP: 127.0.0.1\r\n\r\n", HttpStatus.UNAUTHORIZED),

            Arguments.of("GET /index.html HTTP/1.1\r\nHost: localhost\r\nX-Original-URL: /admin/index.html\r\nX-Rewrite-URL: /admin/index.html\r\n\r\n", HttpStatus.OK),

            Arguments.of("GET /api/authentication/sessions HTTP/1.1\r\nHost: localhost\r\nX-Forwarded-For: 1.2.3.4\r\nX-Real-IP: 5.6.7.8\r\nX-Client-IP: 9.9.9.9\r\nTrue-Client-IP: 8.8.8.8\r\n\r\n", HttpStatus.UNAUTHORIZED),

            Arguments.of("GET /index .html HTTP/1.1\r\nHost: localhost\r\n\r\n", HttpStatus.BAD_REQUEST),
            Arguments.of("GET /index.html?param=\u00e9\u00e8\u00ea HTTP/1.1\r\nHost: localhost\r\n\r\n", HttpStatus.NOT_FOUND)
        );
    }

    ///..
    public static Stream<Arguments> crlfInjectionProvider() {

        waitForInit();

        return Stream.of(

            Arguments.of("GET /index.html\r\nInjected-Header: evil HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.BAD_REQUEST, "Injected-Header: evil"),

            Arguments.of("GET /index.html\r\nInjected-Header: evil HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.BAD_REQUEST, "Injected-Header: evil"),

            Arguments.of("GET /index.html HTTP/1.1\r\nX-Test: value\r\nInjected: header\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.OK, "Injected: header"),

            Arguments.of("GET /index.html HTTP/1.1\r\nUser-Agent: evil\r\nSet-Cookie: hacked=true\r\nHost: localhost\r\n\r\n", HttpStatus.OK, "hacked=true")
        );
    }

    ///..
    public static Stream<Arguments> privilegedPathsProvider() {

        waitForInit();

        return Stream.of(

            Arguments.of("GET /admin/index.html?password=admin HTTP/1.1\r\nHost: localhost\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /admin/index.html#secret HTTP/1.1\r\nHost: localhost\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /admin/index.html HTTP/1.1\r\nHost: localhost\r\n\r\n", HttpStatus.SEE_OTHER),
            Arguments.of("GET /admin/observability/logs.html HTTP/1.1\r\nHost: localhost\r\n\r\n", HttpStatus.SEE_OTHER),
            Arguments.of("GET /prefix/admin/observability/logs.html HTTP/1.1\r\nHost: localhost\r\n\r\n", HttpStatus.NOT_FOUND)
        );
    }

    ///..
    public static Stream<Arguments> methodProvider() {

        waitForInit();

        return Stream.of(

            Arguments.of(HttpMethod.POST),
            Arguments.of(HttpMethod.DELETE)
        );
    }

    ///..
    public static Stream<Arguments> loginProvider() {

        waitForInit();

        return Stream.of(

            Arguments.of("", HttpStatus.UNAUTHORIZED),
            Arguments.of(Application.exposePropertiesForTests().getLoginPassword() + "wrong", HttpStatus.UNAUTHORIZED)
        );
    }

    ///..
    public static Stream<Arguments> apiProvider() {

        waitForInit();

        return Stream.of(

            Arguments.of(Api.GET_SESSIONS.getPath(), null, HttpStatus.OK),
            Arguments.of(Api.GET_LOGS.getPath(), "0|5000000000000|||||", HttpStatus.OK),
            Arguments.of(Api.GET_SYSTEM_METRICS.getPath(), "0|5000000000000|10000000000", HttpStatus.OK),
            Arguments.of(Api.GET_CRAWL_METRICS.getPath(), "0|5000000000000||", HttpStatus.OK),
            Arguments.of(Api.GET_REQUEST_METRICS.getPath(), "0|5000000000000|10000000000", HttpStatus.OK),
            Arguments.of(Api.GET_LOGS.getPath(), "0|5000000000000||||||", HttpStatus.BAD_REQUEST),
            Arguments.of(Api.GET_SYSTEM_METRICS.getPath(), "0|5000000000000|10000000000|", HttpStatus.BAD_REQUEST),
            Arguments.of(Api.GET_CRAWL_METRICS.getPath(), "0|5000000000000|||", HttpStatus.BAD_REQUEST),
            Arguments.of(Api.GET_REQUEST_METRICS.getPath(), "0|5000000000000|10000000000|", HttpStatus.BAD_REQUEST)
        );
    }

    ///
}
