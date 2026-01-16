package io.github.clamentos.gattoslab;

///
import io.github.clamentos.gattoslab.configuration.PropertyProvider;
import io.github.clamentos.gattoslab.contexts.AuthenticatedRequestContext;
import io.github.clamentos.gattoslab.contexts.StaticRequestContext;
import io.github.clamentos.gattoslab.contexts.TestCaseRequestContext;
import io.github.clamentos.gattoslab.session.SessionRole;

///.
import jakarta.el.PropertyNotFoundException;
import jakarta.servlet.http.Cookie;

///.
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

///.
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

///
@SpringBootTest
@TestPropertySource(locations = "classpath:application.yml")
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

///
public class ApplicationTests extends ArgumentsProvider {

    // TODO: test log outs
    // TODO: test metrics

    ///
    private final String cookieName;
    private final String baseUrl;
    private final int ratelimitTokens;
    private final int retryAfter;

    ///..
    private final MockMvc mockMvc;

    ///
    @Autowired
    public ApplicationTests(final PropertyProvider propertyProvider, final WebApplicationContext webApplicationContext) throws PropertyNotFoundException {

        cookieName = propertyProvider.getProperty("app.session.cookieName", String.class);
        baseUrl = "http://localhost:" + propertyProvider.getProperty("server.port", String.class);
        ratelimitTokens = propertyProvider.getProperty("app.ratelimit.maxTokensPerIp", Integer.class);
        retryAfter = propertyProvider.getProperty("app.ratelimit.retryAfter", Integer.class);

        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    ///
    @Test @Order(1)
    @SuppressWarnings("java:S2925")
    public void rateLimitTest() throws Exception {

        final MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(HttpMethod.GET, baseUrl + "/index.html");

        for(int i = 0; i < ratelimitTokens; i++) {

            Assertions.assertEquals(200, mockMvc.perform(request).andReturn().getResponse().getStatus());
        }

        Assertions.assertEquals(429, mockMvc.perform(request).andReturn().getResponse().getStatus());
        Assertions.assertEquals(429, mockMvc.perform(request).andReturn().getResponse().getStatus());

        Thread.sleep(retryAfter + 100);

        Assertions.assertEquals(200, mockMvc.perform(request).andReturn().getResponse().getStatus());
    }

    ///..
    @ParameterizedTest @Order(2)
    @MethodSource("staticSiteTestArgs")
    public void staticSiteTest(final StaticRequestContext context) throws Exception {

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.request(context.getHttpMethod(), baseUrl + context.getPath());
        if(context.isCached()) request = request.header("If-Modified-Since", DateTimeFormatter.RFC_1123_DATE_TIME.format(OffsetDateTime.now()));

        this.check(mockMvc.perform(request).andReturn().getResponse(), context);
    }

    ///..
    @ParameterizedTest @Order(3)
    @MethodSource("adminLoginTestArgs")
    public void adminLoginTest(final AuthenticatedRequestContext context) throws Exception {

        final MockHttpServletRequestBuilder request = MockMvcRequestBuilders

            .request(context.getHttpMethod(), baseUrl + context.getPath())
            .header("Authorization", context.getApiKey())
        ;

        final MockHttpServletResponse response = mockMvc.perform(request).andReturn().getResponse();
        this.check(response, context);

        if(response.getStatus() == 200) {

            final Cookie[] cookies = response.getCookies();
            Assertions.assertTrue(cookies != null && cookies.length > 0, "Null or empty cookies");

            final List<String> names = new ArrayList<>(cookies.length);
            final String expectedName = cookieName + SessionRole.ADMIN;

            Cookie found = null;

            for(final Cookie cookie : cookies) {

                names.add(cookie.getName());

                if(cookie.getName().equals(expectedName)) {

                    found = cookie;
                    break;
                }
            }

            Assertions.assertNotNull(found, "No cookie with name " + cookieName + " found. Cookies got are: " + names);
            Assertions.assertTrue(found.getValue() != null && !found.getValue().isEmpty(), "Malformed cookie. Value is: " + found.getValue());
        }
    }

    ///.
    private void check(final MockHttpServletResponse response, final TestCaseRequestContext context) throws Exception {

        final HttpStatus responseStatus = HttpStatus.valueOf(response.getStatus());
        final String responseContentType = (String)response.getHeaderValue("Content-Type");

        final boolean statusCheck = context.getExpectedStatuses().contains(responseStatus);
        final boolean contentTypeCheck = context.getExpectedContentTypes().isEmpty() || context.getExpectedContentTypes().contains(responseContentType);

        Assertions.assertEquals(true, statusCheck, "Unexpected status. Got: " + responseStatus + ", Response body is: " + response.getContentAsString());
        Assertions.assertEquals(true, contentTypeCheck, "Unexpected content type. Got: " + responseContentType);
    }

    ///
}
