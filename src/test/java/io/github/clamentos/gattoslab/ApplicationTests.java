package io.github.clamentos.gattoslab;

///
import io.github.clamentos.gattoslab.configuration.PropertyProvider;

///.
import jakarta.el.PropertyNotFoundException;

///.
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

///.
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

///.
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

///
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application.yml")

///
public class ApplicationTests extends ArgumentsProvider {

	///
	private final String baseUrl;
	private final MockMvc mockMvc;

	///
	@Autowired
	public ApplicationTests(final MockMvc mockMvc, final PropertyProvider propertyProvider) throws PropertyNotFoundException {

		baseUrl = "http://localhost:" + propertyProvider.getProperty("server.port", String.class);
		this.mockMvc = mockMvc;
	}

	///
	@ParameterizedTest
	@MethodSource("staticSiteTestArgs")
	public void staticSiteTest(final Map<String, Object> context) throws Exception {

		// TODO: test actual content: mime

		final String now = DateTimeFormatter.RFC_1123_DATE_TIME.format(OffsetDateTime.now());
		var request = MockMvcRequestBuilders.request((HttpMethod)context.get("method"), baseUrl + (String)context.get("path"));
		if((boolean)context.get("cache")) request = request.header("If-Modified-Since", now);

		final int status = mockMvc.perform(request).andReturn().getResponse().getStatus();
		Assertions.assertEquals(context.get("status"), status);
	}

	///..
	@ParameterizedTest
	@MethodSource("adminLoginTestArgs")
	public void adminLoginTest(final Map<String, Object> context) throws Exception {

		final int status = mockMvc

			.perform(MockMvcRequestBuilders

				.request((HttpMethod)context.get("method"), baseUrl + (String)context.get("path"))
				.header("Authorization", (String)context.get("apiKey"))
			)
			.andReturn()
			.getResponse()
			.getStatus()
		;

		Assertions.assertEquals(context.get("status"), status);
	}

	///
}
