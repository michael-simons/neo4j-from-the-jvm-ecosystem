package org.neo4j.examples.jvm.tck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.relaxedResponseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.examples.jvm.tck.movies.Actor;
import org.neo4j.examples.jvm.tck.movies.Movie;
import org.neo4j.examples.jvm.tck.movies.MovieService;
import org.neo4j.examples.jvm.tck.movies.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.operation.preprocess.ContentModifier;
import org.springframework.restdocs.operation.preprocess.ContentModifyingOperationPreprocessor;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Michael J. Simons
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureRestDocs
class TckTest {

	@BeforeEach
	void prepareDatabase(@Autowired MovieService movieService) {

		movieService.prepareDatabase();
	}

	private final static int NUMBER_OF_INITIAL_MOVIES = 38;

	@Test
	@DisplayName("POST /api/people")
	void verifyPostPerson(@Autowired WebTestClient webclient, @Autowired Driver driver) {

		var newPerson = webclient.post().uri("/api/people")
			.bodyValue(new Person("Lieschen Müller", 2020))
			.exchange()
			.expectStatus().isCreated()
			.expectBody(Person.class)
			.value(p -> {
				assertThat(p.id()).isNotNull();
				assertThat(p.born()).isEqualTo(2020);
				assertThat(p.name()).isEqualTo("Lieschen Müller");
			})
			.consumeWith(document("people",
				preprocessResponse(prettyPrint()),
				requestFields(
					fieldWithPath("born").description("The year in which the person was born."),
					fieldWithPath("name").description("The name of the person.")
				),
				responseFields(
					fieldWithPath("born").description("The year in which the person was born."),
					fieldWithPath("name").description("The name of the person."),
					fieldWithPath("id").description("The neo4j internal id."))
				)
			)
			.returnResult()
			.getResponseBody();

		try (var session = driver.session()) {
			var cnt = session.run("MATCH (n) WHERE id(n) = $id RETURN count(n)", Map.of("id", newPerson.id()))
				.single().get(0).asLong();
			assertThat(cnt).isEqualTo(1L);
		}
	}

	@Test
	@DisplayName("GET /api/movies")
	void verifyGetListOfMovies(@Autowired WebTestClient webclient) {

		var movies = webclient.get().uri("/api/movies")
			.exchange()
			.expectStatus().isOk()
			.expectHeader().value(HttpHeaders.CONTENT_TYPE,
				s -> {
					var mediaType = MediaType.parseMediaType(s);
					var compatibleMediaTypeReturned = mediaType.isCompatibleWith(MediaType.TEXT_EVENT_STREAM) ||
													  mediaType.isCompatibleWith(MediaType.APPLICATION_JSON);
					assertThat(compatibleMediaTypeReturned).isTrue();
				})
			.expectBodyList(Movie.class)
			.consumeWith(document("movies", preprocessResponse(
				new ContentModifyingOperationPreprocessor(new JsonEventStreamToJsonArray(1)),
				prettyPrint()
			), responseFields(
				fieldWithPath("[].title").description("The title of the movie"),
				fieldWithPath("[].description").description("The movies description").optional(),
				fieldWithPath("[].actors").description("The actors in that movie"),
				fieldWithPath("[].released").description("The year the movie was released"),
				fieldWithPath("[].actors[].name").description("Name of the actor"),
				fieldWithPath("[].actors[].roles").description("Roles played"),
				fieldWithPath("[].directors[]").description("The directors of that movie"),
				fieldWithPath("[].directors[].born").description("The year in which the person was born."),
				fieldWithPath("[].directors[].name").description("The name of the person."),
				fieldWithPath("[].directors[].id").description("The neo4j internal id.")
			)))
			.returnResult()
			.getResponseBody();

		assertMovieList(movies);
	}

	@Test
	@DisplayName("GET /management/health/liveness")
	void verifyLiveness(@Autowired WebTestClient webclient) {

		webclient.get().uri("/management/health/liveness")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
			.expectBody()
			.jsonPath("status").isEqualTo("UP")
			.consumeWith(document("health/liveness", preprocessResponse(
				prettyPrint()
			), relaxedResponseFields(
				fieldWithPath("status").description("Whether the application can work correctly or not.")
			)));
	}

	@Test
	@DisplayName("GET /management/health/readiness")
	void verifyReadiness(@Autowired WebTestClient webclient) {

		webclient.get().uri("/management/health/readiness")
			.accept(MediaType.APPLICATION_JSON)
			.exchange()
			.expectStatus().isOk()
			.expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
			.expectBody()
			.jsonPath("status").isEqualTo("UP")
			.consumeWith(document("health/readiness", preprocessResponse(
				prettyPrint()
			), relaxedResponseFields(
				fieldWithPath("status").description(
					"The “Readiness” state of an application tells whether the application is ready to handle traffic. A failing “Readiness” state tells the platform that it should not route traffic to the application for now.")
			)));
	}

	private void assertMovieList(List<Movie> movies) {

		Consumer<Movie> theMovieCloudAtlas = m -> {
			assertThat(m.actors())
				.extracting(Actor::name)
				.containsExactlyInAnyOrder("Hugo Weaving", "Jim Broadbent", "Tom Hanks", "Halle Berry");

			assertThat(m.actors())
				.filteredOn(Actor::name, "Halle Berry")
				.singleElement()
				.extracting(Actor::roles).asList()
				.containsExactlyInAnyOrder("Luisa Rey", "Jocasta Ayrs", "Ovid", "Meronym");

			assertThat(m.directors())
				.allSatisfy(p -> assertThat(p.id()).isNotNull())
				.extracting(Person::name)
				.containsExactlyInAnyOrder("Lana Wachowski", "Lilly Wachowski", "Tom Tykwer");
		};

		assertThat(movies)
			.hasSize(NUMBER_OF_INITIAL_MOVIES)
			.filteredOn(Movie::title, "Cloud Atlas")
			.singleElement()
			.satisfies(theMovieCloudAtlas);
	}

	@SpringBootApplication
	@ComponentScan(excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = {
		Application.class }))
	static class Dummy {
	}

	@TestConfiguration
	static class DefaultTestConfig {

		/**
		 * The url should be configured to point to the correct project
		 *
		 * @return A web test client
		 */
		@Bean
		WebTestClient webTestClient(
			@Value("${host-under-test:localhost:8080}") String hostUnderTest,
			RestDocumentationContextProvider restDocumentation,
			ObjectMapper objectMapper
		) {

			return WebTestClient
				.bindToServer()
				.baseUrl("http://%s".formatted(hostUnderTest))
				.filter(documentationConfiguration(restDocumentation))
				.responseTimeout(Duration.ofSeconds(30)) // Hopefully high enough that the container under tests are up
				.codecs(c -> {
					c.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
					c.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
				})
				.build();
		}
	}

	private static class JsonEventStreamToJsonArray implements ContentModifier {

		private final int limit;

		JsonEventStreamToJsonArray() {
			this(Integer.MAX_VALUE);
		}

		JsonEventStreamToJsonArray(int limit) {
			this.limit = limit;
		}

		@Override
		public byte[] modifyContent(byte[] bytes, MediaType mediaType) {
			if (!MediaType.TEXT_EVENT_STREAM.isCompatibleWith(mediaType)) {
				return bytes;
			}

			var charset = Optional.ofNullable(mediaType.getCharset()).orElse(StandardCharsets.UTF_8);
			var result = new StringBuilder().append("[");
			int cnt = 0;
			try (var scanner = new Scanner(new ByteArrayInputStream(bytes), charset).useDelimiter("(\r?\n){2}")) {
				while (scanner.hasNext()) {
					var item = scanner.next().trim().replaceAll("^data: *", "").trim();
					if (item.isEmpty()) {
						continue;
					}
					result.append(item).append(",");
					if (++cnt >= limit) {
						break;
					}
				}
			}
			result.replace(result.length() - 1, result.length(), "]");
			return result.toString().getBytes(charset);
		}
	}
}
