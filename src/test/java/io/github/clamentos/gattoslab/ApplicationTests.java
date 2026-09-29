package io.github.clamentos.gattoslab;

///
import io.github.clamentos.gattoslab.configuration.ApplicationProperties;
import io.github.clamentos.gattoslab.exchange.filters.components.AuthorizationAction;
import io.github.clamentos.gattoslab.exchange.handling.components.Api;
import io.github.clamentos.gattoslab.exchange.handling.components.StaticResource;
import io.github.clamentos.gattoslab.http.HttpMethod;
import io.github.clamentos.gattoslab.http.HttpStatus;
import io.github.clamentos.gattoslab.utils.GenericUtils;

///..
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;

///..
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

///
@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)

///
class ApplicationTests {

    ///
    private static final String BASE_URI = "http://localhost:8080";
    private static final String HOST = "localhost";
    private static final int PORT = 8080;
    private static final int TIMEOUT = 5000;
    private static final HttpClient httpClient = HttpClient.newBuilder().build();

    ///
    private static HttpResponse<byte[]> exchange(final String path, final HttpMethod httpMethod) throws Exception {

        final HttpRequest request = HttpRequest

            .newBuilder()
            .uri(URI.create(BASE_URI + path))
            .method(httpMethod.name(), BodyPublishers.noBody())
            .build()
        ;

        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    ///..
    private static HttpResponse<byte[]> exchange(final String path, final HttpMethod httpMethod, final Map<String, String> headers)
    throws Exception {

        final HttpRequest.Builder request = HttpRequest

            .newBuilder()
            .uri(URI.create(BASE_URI + path))
            .method(httpMethod.name(), BodyPublishers.noBody())
        ;

        for(final Entry<String, String> header : headers.entrySet()) {

            request.setHeader(header.getKey(), header.getValue());
        }

        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    ///..
    private static void checkContent(final byte[] received, final String path) throws IOException {

        final StaticResource resource = (StaticResource)Application.exposeMappingsForTests().get(path);

        if(resource.getDiskPath() == null) {

            final byte[] expected = resource.getCompressedContent();
            Assertions.assertTrue(Arrays.equals(expected, received));
        }

        else {

            try(final GZIPInputStream decompressor = new GZIPInputStream(new ByteArrayInputStream(received))) {

                Assertions.assertTrue(Arrays.equals(

                    ApplicationTests.class.getClassLoader().getResourceAsStream(resource.getDiskPath()).readAllBytes(),
                    decompressor.readAllBytes()
                ));
            }
        }
    }

    ///..
    private static byte[] rawRequest(final byte[] requestBytes) throws IOException {

        return rawRequest(requestBytes, TIMEOUT);
    }

    ///..
    private static byte[] rawRequest(final byte[] requestBytes, final int timeout) throws IOException {

        try(final Socket socket = new Socket()) {

            socket.connect(new InetSocketAddress(HOST, PORT), timeout);
            socket.setSoTimeout(timeout);

            final OutputStream out = socket.getOutputStream();

            out.write(requestBytes);
            out.flush();

            return socket.getInputStream().readAllBytes();
        }
    }

    ///..
    private static byte[] createWeirdRequest(final String method, final String path) {

        return (method + " " + path + " HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n").getBytes(StandardCharsets.UTF_8);
    }

    ///..
    private static int extractStatusCode(final byte[] response) {

        return Integer.parseInt(GenericUtils.fastSplit(new String(response).split("\r\n")[0], ' ').get(1));
    }

    ///
    @ParameterizedTest @Order(0)
    @MethodSource("io.github.clamentos.gattoslab.ArgumentProviders#staticPathsProvider")
    void testStaticResources(final String path, final AuthorizationAction authorizationAction) throws Exception {

        final HttpResponse<byte[]> response = exchange(path, HttpMethod.GET);

        Assertions.assertEquals(

            authorizationAction != AuthorizationAction.ALLOW ?

                HttpStatus.SEE_OTHER.getCode() :
                HttpStatus.OK.getCode()
            ,

            response.statusCode()
        );

        final byte[] body = response.body();

        if(response.statusCode() == HttpStatus.OK.getCode()) checkContent(body, path);
        else Assertions.assertEquals(0, body.length);
    }

    ///..
    @ParameterizedTest @Order(0)
    @MethodSource("io.github.clamentos.gattoslab.ArgumentProviders#apiPathsProvider")
    void testApisNonAuthenticated(final String path) throws Exception {

        final HttpResponse<?> response = exchange(path, HttpMethod.GET);
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED.getCode(), response.statusCode());
    }

    ///..
    @ParameterizedTest @Order(0)
    @MethodSource("io.github.clamentos.gattoslab.ArgumentProviders#weirdPathProvider")
    void testWeirdPaths(final String path) throws Exception {

        final byte[] response = rawRequest(createWeirdRequest("GET", path));

        Assertions.assertTrue(response.length > 0);
        Assertions.assertEquals(HttpStatus.NOT_FOUND.getCode(), extractStatusCode(response));
    }

    ///..
    @ParameterizedTest @Order(0)
    @MethodSource("io.github.clamentos.gattoslab.ArgumentProviders#weirdRequestProvider")
    void testMalformedFirstLine(final String firstLine, final HttpStatus httpStatus) throws Exception {

        final byte[] response = rawRequest(firstLine.getBytes());

        Assertions.assertTrue(response.length > 0);
        Assertions.assertEquals(httpStatus.getCode(), extractStatusCode(response));
    }

    ///..
    @Test @Order(0)
    void testWeirdMethod() throws Exception {

        final byte[] response = rawRequest(createWeirdRequest("FOO", "/index.html"));

        Assertions.assertTrue(response.length > 0);
        Assertions.assertEquals(HttpStatus.METHOD_NOT_ALLOWED.getCode(), extractStatusCode(response));
    }

    ///..
    @Test @Order(0)
    void testPathTraversalRawNullByte() throws Exception {

        // GET /index.html<0x00>.txt HTTP/1.1 (raw null byte in path)
        final byte[] path = new byte[]{

            (byte)'/', (byte)'i', (byte)'n', (byte)'d', (byte)'e', (byte)'x', (byte)'.',
            (byte)'h', (byte)'t', (byte)'m', (byte)0x00, (byte)'.', (byte)'t', (byte)'x', (byte)'t'
        };

        final byte[] prefix = "GET ".getBytes();
        final byte[] suffix = " HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n".getBytes(StandardCharsets.US_ASCII);

        final byte[] fullRequest = new byte[prefix.length + path.length + suffix.length];
        System.arraycopy(prefix, 0, fullRequest, 0, prefix.length);
        System.arraycopy(path, 0, fullRequest, prefix.length, path.length);
        System.arraycopy(suffix, 0, fullRequest, prefix.length + path.length, suffix.length);

        final byte[] response = rawRequest(fullRequest);

        Assertions.assertTrue(response.length > 0);
        Assertions.assertEquals(HttpStatus.NOT_FOUND.getCode(), extractStatusCode(response));
    }

    ///..
    @ParameterizedTest @Order(0)
    @MethodSource("io.github.clamentos.gattoslab.ArgumentProviders#hugeLineProvider")
    void testMalformedHugeFirstLine(final int amount, final HttpStatus httpStatus) throws Exception {

        final StringBuilder sb = new StringBuilder("GET /");
        for(int i = 0; i < amount; i++) sb.append('A');
        sb.append(" HTTP/1.1\r\nUser-Agent: junit-tests\r\n\r\n");

        final byte[] response = rawRequest(sb.toString().getBytes());

        Assertions.assertTrue(response.length > 0);
        Assertions.assertEquals(httpStatus.getCode(), extractStatusCode(response));
    }

    ///..
    @ParameterizedTest @Order(0)
    @MethodSource("io.github.clamentos.gattoslab.ArgumentProviders#hugeHeaderProvider")
    void testMalformedManyHeaders(final int amount, final HttpStatus httpStatus) throws Exception {

        final StringBuilder sb = new StringBuilder("GET /index.html HTTP/1.1\r\n");

        for(int i = 0; i < amount; i++) {

            sb.append("X-Header-").append(i).append(": ");

            for(int j = 0; j < 10; j++) {

                sb.append('A');
            }

            sb.append("\r\n");
        }

        sb.append("\r\n");

        final byte[] request = sb.toString().getBytes();

        try {

            final byte[] response = rawRequest(request);

            Assertions.assertTrue(response.length > 0);
            Assertions.assertEquals(httpStatus.getCode(), extractStatusCode(response));
        }

        catch(final IOException exc) {

            Assertions.assertTrue(exc.getMessage().contains("pipe"));
        }

        // Verify server is still responsive
        final HttpResponse<?> response = exchange("/index.html", HttpMethod.GET);
        Assertions.assertEquals(HttpStatus.OK.getCode(), response.statusCode());
    }

    ///..
    @ParameterizedTest @Order(0)
    @MethodSource("io.github.clamentos.gattoslab.ArgumentProviders#hugeHeaderProvider")
    void testMalformedHugeHeader(final int amount, final HttpStatus httpStatus) throws Exception {

        final StringBuilder sb = new StringBuilder("GET /index.html HTTP/1.1\r\nUser-Agent: ");

        for(int i = 0; i < amount; i++) {

            sb.append('A');
        }

        sb.append("\r\n\r\n");

        final byte[] request = sb.toString().getBytes();
        final byte[] response = rawRequest(request);

        Assertions.assertTrue(response.length > 0);
        Assertions.assertEquals(httpStatus.getCode(), extractStatusCode(response));
    }

    ///..
    @ParameterizedTest @Order(0)
    @MethodSource("io.github.clamentos.gattoslab.ArgumentProviders#crlfInjectionProvider")
    void testCrlfInjection(final String request, final HttpStatus httpStatus, final String contains) throws Exception {

        final byte[] response = rawRequest(request.getBytes());

        Assertions.assertTrue(response.length > 0);
        Assertions.assertEquals(httpStatus.getCode(), extractStatusCode(response));
        Assertions.assertFalse(new String(response).contains(contains));
    }

    ///..
    @ParameterizedTest @Order(0)
    @MethodSource("io.github.clamentos.gattoslab.ArgumentProviders#privilegedPathsWithComponentsProvider")
    void testPrivilegedPathsWithComponents(final String request) throws Exception {

        final byte[] response = rawRequest(request.getBytes());

        Assertions.assertTrue(response.length > 0);
        Assertions.assertEquals(HttpStatus.SEE_OTHER.getCode(), extractStatusCode(response));
    }

    ///..
    @ParameterizedTest @Order(0)
    @MethodSource("io.github.clamentos.gattoslab.ArgumentProviders#methodProvider")
    void testIllegalMethod(final HttpMethod httpMethod) throws Exception {

        final byte[] response = rawRequest((httpMethod.name() + " /index.html HTTP/1.1\r\nHost: localhost\r\n\r\n").getBytes());

        Assertions.assertTrue(response.length > 0);
        Assertions.assertEquals(HttpStatus.METHOD_NOT_ALLOWED.getCode(), extractStatusCode(response));
    }

    ///..
    @Test @Order(0)
    void testOptionsToAdminPath() throws Exception {

        final byte[] response = rawRequest("OPTIONS /admin/index.html HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes());

        Assertions.assertTrue(response.length > 0);
        Assertions.assertEquals(HttpStatus.NO_CONTENT.getCode(), extractStatusCode(response));
    }

    ///..
    @Test @Order(0)
    void testServerSurvivesRapidConnections() throws Exception {

        // Open and close many connections rapidly
        for(int i = 0; i < 200; i++) {

            try(final Socket socket = new Socket()) {

                socket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT);
                socket.getOutputStream().write("GET /index.html HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes());
                socket.getOutputStream().flush();
            }
        }

        // Verify server is still responsive
        final HttpResponse<?> response = exchange("/index.html", HttpMethod.GET);
        Assertions.assertEquals(HttpStatus.OK.getCode(), response.statusCode());
    }

    ///..
    @Test @Order(0)
    void testServerSurvivesHalfOpenConnection() throws Exception {

        // Open a connection, send partial request, wait, then send the rest
        try(final Socket socket = new Socket()) {

            socket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT);
            socket.setSoTimeout(TIMEOUT);

            final OutputStream out = socket.getOutputStream();

            out.write("GET /index.html HTTP/1.1\r\nHost: local".getBytes());
            out.flush();

            GenericUtils.silentSleep(1000);

            out.write("host\r\n\r\n".getBytes());
            out.flush();

            socket.getInputStream().readAllBytes();
        }

        // Verify server is still responsive
        final HttpResponse<?> response = exchange("/index.html", HttpMethod.GET);
        Assertions.assertEquals(HttpStatus.OK.getCode(), response.statusCode());
    }

    ///..
    @Test @Order(0)
    void testServerSurvivesSlowLoris() throws Exception {

        // Send headers very slowly (slow loris attack)
        try(final Socket socket = new Socket()) {

            socket.connect(new InetSocketAddress(HOST, PORT), TIMEOUT);
            socket.setSoTimeout(20000);

            final OutputStream out = socket.getOutputStream();

            for(final char c : "GET /index.html HTTP/1.1\r\n".toCharArray()) {

                out.write(c);
                out.flush();

                GenericUtils.silentSleep(50);
            }

            for(final char c : "Host: localhost\r\n\r\n".toCharArray()) {

                out.write(c);
                out.flush();

                GenericUtils.silentSleep(50);
            }

            socket.getInputStream().readAllBytes();
        }

        // Verify server is still responsive
        final HttpResponse<?> response = exchange("/index.html", HttpMethod.GET);
        Assertions.assertEquals(HttpStatus.OK.getCode(), response.statusCode());
    }

    ///..
    @Test @Order(0)
    void testConcurrentRequestsToAdminPath() throws Exception {

        final List<CompletableFuture<Integer>> futures = new ArrayList<>();

        for(int i = 0; i < 100; i++) {

            futures.add(CompletableFuture.supplyAsync(() -> {

                try {

                    final HttpResponse<?> response = exchange("/admin/index.html", HttpMethod.GET);
                    return response.statusCode();
                }

                catch(final Exception _) {

                    return -1;
                }
            }));
        }

        for(final CompletableFuture<Integer> future : futures) {

            final int status = future.get(10, TimeUnit.SECONDS);
            Assertions.assertEquals(HttpStatus.SEE_OTHER.getCode(), status);
        }
    }

    ///..
    @Test @Order(0)
    void testConcurrentMalformedAndValidRequests() throws Exception {

        final List<CompletableFuture<Boolean>> futures = new ArrayList<>();

        for(int i = 0; i < 5; i++) {

            futures.add(CompletableFuture.supplyAsync(() -> {

                try {

                    final HttpResponse<?> response = exchange("/index.html", HttpMethod.GET);
                    return response.statusCode() == HttpStatus.OK.getCode();
                }

                catch(final Exception _) {

                    return false;
                }
            }));

            futures.add(CompletableFuture.supplyAsync(() -> {

                try {

                    final byte[] response = rawRequest("GARBAGE\r\n\r\n".getBytes(), TIMEOUT);
                    return extractStatusCode(response) == HttpStatus.BAD_REQUEST.getCode();
                }

                catch(final IOException _) {

                    return true;
                }

                catch(final Exception _) {

                    return false;
                }
            }));
        }

        for(final CompletableFuture<Boolean> future : futures) {

            future.get(10, TimeUnit.SECONDS);
        }

        // Verify server is still responsive
        final HttpResponse<?> response = exchange("/index.html", HttpMethod.GET);
        Assertions.assertEquals(HttpStatus.OK.getCode(), response.statusCode());
    }

    ///..
    @Test @Order(0)
    void testUtf8BomInPath() throws Exception {

        // UTF-8 BOM (EF BB BF) at the start of the path
        final byte[] bom = {(byte)0xEF, (byte)0xBB, (byte)0xBF};
        final byte[] path = "/index.html".getBytes();
        final byte[] prefix = "GET ".getBytes();
        final byte[] suffix = " HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes();

        final byte[] request = new byte[prefix.length + bom.length + path.length + suffix.length];
        int offset = 0;

        System.arraycopy(prefix, 0, request, offset, prefix.length);
        offset += prefix.length;
        System.arraycopy(bom, 0, request, offset, bom.length);
        offset += bom.length;
        System.arraycopy(path, 0, request, offset, path.length);
        offset += path.length;
        System.arraycopy(suffix, 0, request, offset, suffix.length);

        final byte[] response = rawRequest(request);
        final int code = extractStatusCode(response);

        Assertions.assertTrue(response.length > 0);
        Assertions.assertTrue(code == HttpStatus.NOT_FOUND.getCode() || code == HttpStatus.BAD_REQUEST.getCode());
    }

    ///..
    @Test @Order(0)
    void testInvalidUtf8Sequence() throws Exception {

        // Invalid UTF-8 byte sequence in path (0xFF 0xFE is not valid UTF-8)
        final byte[] path = new byte[]{(byte)'/', (byte)0xFF, (byte)0xFE, (byte)'.', (byte)'h', (byte)'t', (byte)'m', (byte)'l'};
        final byte[] prefix = "GET ".getBytes(StandardCharsets.US_ASCII);
        final byte[] suffix = " HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes();

        final byte[] request = new byte[prefix.length + path.length + suffix.length];

        System.arraycopy(prefix, 0, request, 0, prefix.length);
        System.arraycopy(path, 0, request, prefix.length, path.length);
        System.arraycopy(suffix, 0, request, prefix.length + path.length, suffix.length);

        final byte[] response = rawRequest(request);
        final int code = extractStatusCode(response);

        Assertions.assertTrue(response.length > 0);
        Assertions.assertTrue(code == HttpStatus.NOT_FOUND.getCode() || code == HttpStatus.BAD_REQUEST.getCode());
    }

    ///..
    @Test @Order(0)
    void testLargeRequestBodyToApi() throws Exception {

        final StringBuilder request = new StringBuilder("POST /api/authentication/login HTTP/1.1\r\nHost: localhost\r\n\r\n");

        for(int i = 0; i < 100000; i++) {

            request.append("A");
        }

        final byte[] response = rawRequest(request.toString().getBytes());

        Assertions.assertTrue(response.length > 0);
        Assertions.assertEquals(HttpStatus.UNAUTHORIZED.getCode(), extractStatusCode(response));
    }

    ///..
    @Test @Order(0)
    void testPipelinedRequests() throws Exception {

        // Send multiple requests without waiting for responses (HTTP pipelining)
        final byte[] request = "GET /index.html HTTP/1.1\r\nHost: localhost\r\n\r\nGET /sitemap.xml HTTP/1.1\r\nHost: localhost\r\n\r\nGET /favicon.ico HTTP/1.1\r\nHost: localhost\r\n\r\n".getBytes();

        final byte[] response = rawRequest(request);
        Assertions.assertTrue(response.length > 0);

        final long responseCount = new String(response).split("HTTP/1\\.").length - 1;
        Assertions.assertEquals(1, responseCount);
    }

    ///..
    @ParameterizedTest() @Order(1)
    @MethodSource("io.github.clamentos.gattoslab.ArgumentProviders#loginProvider")
    void testLoginFail(final String password, final HttpStatus status) throws Exception {

        final HttpResponse<?> response = exchange(

            Api.LOGIN.getPath(),
            HttpMethod.POST,
            Map.of("Authorization", password)
        );

        Assertions.assertEquals(status.getCode(), response.statusCode());
        Assertions.assertTrue(response.headers().firstValue("Set-Cookie").isEmpty());
    }

    ///..
    @ParameterizedTest() @Order(2)
    @MethodSource("io.github.clamentos.gattoslab.ArgumentProviders#apiProvider")
    void testApiOk(final String path, final String filterOptional, final HttpStatus status) throws Exception {

        final HttpResponse<?> loginResponse = exchange(

            Api.LOGIN.getPath(),
            HttpMethod.POST,
            Map.of("Authorization", Application.exposePropertiesForTests().getLoginPassword())
        );

        Assertions.assertEquals(HttpStatus.OK.getCode(), loginResponse.statusCode());

        final String sessionCookieRaw = loginResponse.headers().firstValue("Set-Cookie").get();
        final String sessionCookie = sessionCookieRaw.substring(0, sessionCookieRaw.length() - 1);

        Assertions.assertTrue(sessionCookie.contains(ApplicationProperties.SESSION_COOKIE_NAME));

        final HttpResponse<byte[]> apiResponse = exchange(

            path + (filterOptional != null ? filterOptional : ""),
            HttpMethod.GET,
            Map.of("Cookie", sessionCookie)
        );

        Assertions.assertEquals(status.getCode(), apiResponse.statusCode());
        if(status == HttpStatus.OK) Assertions.assertTrue(apiResponse.body().length > 0);

        final HttpResponse<?> logoutResponse = exchange(

            Api.LOGOUT.getPath(),
            HttpMethod.DELETE,
            Map.of("Cookie", sessionCookie)
        );

        Assertions.assertEquals(HttpStatus.OK.getCode(), logoutResponse.statusCode());
    }

    ///..
    @Test @Order(3)
    void testRateLimit() throws Exception {

        int requestCount = 0;
        int okCount = 0;
        boolean rateLimited = false;

        while(!rateLimited) {

            final HttpResponse<?> response = exchange("/index.html", HttpMethod.GET);

            if(response.statusCode() == HttpStatus.TOO_MANY_REQUESTS.getCode()) rateLimited = true;
            else if(response.statusCode() == HttpStatus.OK.getCode()) okCount++;

            requestCount++;
        }

        Assertions.assertEquals(okCount, requestCount - 1);
    }

    ///
}
