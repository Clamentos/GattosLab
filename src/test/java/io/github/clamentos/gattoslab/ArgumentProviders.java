package io.github.clamentos.gattoslab;

import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
///
import io.github.clamentos.gattoslab.exchange.handling.components.Api;
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

        while(

            Application.exposeMappingsForTests() == null ||
            Application.exposePropertiesForTests() == null ||
            Application.exposeShutdownHookForTests() == null
        ) {

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
    public static Stream<Arguments> hugeLineProvider() {

        waitForInit();

        return Stream.of(

            Arguments.of(ApplicationProperties.MAX_REQUEST_SIZE >> 1, HttpStatus.NOT_FOUND),
            Arguments.of(ApplicationProperties.MAX_REQUEST_SIZE << 1, HttpStatus.CONTENT_TOO_LARGE)
        );
    }

    ///..
    public static Stream<Arguments> hugeHeaderProvider() {

        waitForInit();

        return Stream.of(

            Arguments.of(ApplicationProperties.MAX_REQUEST_SIZE >> 7, HttpStatus.OK),
            Arguments.of(ApplicationProperties.MAX_REQUEST_SIZE << 1, HttpStatus.CONTENT_TOO_LARGE)
        );
    }

    ///..
    public static Stream<Arguments> explicitRequestProvider() {

        waitForInit();

        return Stream.of(

            Arguments.of("GET /\u00e9.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /\u0444\u0430\u0439\u043b.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /\u65e5\u672c\u8a9e.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /\ud83c\udf89.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /\u0645\u0631\u062d\u0628\u0627.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /index\u00e9.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /../../etc/passwd HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /..%2f..%2f..%2fetc%2fpasswd HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /..%252f..%252f..%252fetc%252fpasswd HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /index.html%00.txt HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /admin/../index.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /admin/..%2f..%2findex.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /Admin/index.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /admin/ HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET //admin/index.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /./admin/index.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /api/authentication/sessions/ HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /a\u0301min/index.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /api/Authentication/Sessions HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /index\t.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /index\u2028.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /in\u200bdex.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET http://localhost:8080/index.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET localhost:8080 HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /../../src/main/java/io/github/clamentos/gattoslab/Application.java HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /../../src/main/resources/application-prod.properties HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /admin%2Findex.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /admin\\..\\..\\etc\\passwd HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /%252e%252e%252f%252e%252e%252fetc%252fpasswd HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NOT_FOUND),

            Arguments.of("POST /index.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.METHOD_NOT_ALLOWED),
            Arguments.of("DELETE /index.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.METHOD_NOT_ALLOWED),
            Arguments.of("FOO /index.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.METHOD_NOT_ALLOWED),
            Arguments.of("OPTIONS /index.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NO_CONTENT),
            Arguments.of("OPTIONS /admin/index.html HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n", HttpStatus.NO_CONTENT),

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
            Arguments.of("GET /index.html?param=\u00e9\u00e8\u00ea HTTP/1.1\r\nHost: localhost\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /index.html HTTP/1.1\r\nUser-Agent: bad-string\r\n\r\n", HttpStatus.FORBIDDEN),
            Arguments.of("GET /index.html HTTP/1.1\r\nUser-Agent: very-bad-string\r\n\r\n", HttpStatus.FORBIDDEN),

            Arguments.of("GET /admin/index.html?password=admin HTTP/1.1\r\nHost: localhost\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /admin/index.html#secret HTTP/1.1\r\nHost: localhost\r\n\r\n", HttpStatus.NOT_FOUND),
            Arguments.of("GET /admin/index.html HTTP/1.1\r\nHost: localhost\r\n\r\n", HttpStatus.SEE_OTHER),
            Arguments.of("GET /admin/observability/logs.html HTTP/1.1\r\nHost: localhost\r\n\r\n", HttpStatus.SEE_OTHER),
            Arguments.of("GET /prefix/admin/observability/logs.html HTTP/1.1\r\nHost: localhost\r\n\r\n", HttpStatus.NOT_FOUND)
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
