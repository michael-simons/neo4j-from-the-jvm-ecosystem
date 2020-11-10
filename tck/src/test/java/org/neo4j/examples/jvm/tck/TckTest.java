package org.neo4j.examples.jvm.tck;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.document;
import static org.springframework.restdocs.webtestclient.WebTestClientRestDocumentation.documentationConfiguration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
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
import org.neo4j.examples.jvm.tck.movies.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
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
	void prepareDatabase(@Autowired Driver driver, @Value("classpath:movies.cypher") Resource movies) {

		try (
			var session = driver.session();
			var tx = session.beginTransaction()
		) {
			tx.run("MATCH (n) DETACH DELETE n");
			readStatements(movies).forEach(tx::run);
			tx.commit();
		}
	}

	private final static int NUMBER_OF_INITIAL_MOVIES = 38;

	@Test
	@DisplayName("POST /api/people")
	void verifyPostPerson(@Autowired WebTestClient webclient, @Autowired Driver driver) {

		var newPerson = webclient.post().uri("/people")
			.bodyValue(Person.builder().withName("Lieschen Müller").withBorn(2020).build())
			.exchange()
			.expectStatus().isCreated()
			.expectBody(Person.class)
			.value(p -> {
				assertThat(p.getId()).isNotNull();
				assertThat(p.getBorn()).isEqualTo(2020);
				assertThat(p.getName()).isEqualTo("Lieschen Müller");
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
			var cnt = session.run("MATCH (n) WHERE id(n) = $id RETURN count(n)", Map.of("id", newPerson.getId()))
				.single().get(0).asLong();
			assertThat(cnt).isEqualTo(1L);
		}
	}

	@Test
	@DisplayName("GET /api/movies")
	void verifyGetListOfMovies(@Autowired WebTestClient webclient) {

		var movies = webclient.get().uri("/movies")
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

	private void assertMovieList(List<Movie> movies) {

		Consumer<Movie> theMovieCloudAtlas = m -> {
			assertThat(m.getActors())
				.extracting(Actor::getName)
				.containsExactlyInAnyOrder("Hugo Weaving", "Jim Broadbent", "Tom Hanks", "Halle Berry");

			assertThat(m.getActors())
				.filteredOn(Actor::getName, "Halle Berry")
				.singleElement()
				.extracting(Actor::getRoles).asList()
				.containsExactlyInAnyOrder("Luisa Rey", "Jocasta Ayrs", "Ovid", "Meronym");

			assertThat(m.getDirectors())
				.allSatisfy(p -> assertThat(p.getId()).isNotNull())
				.extracting(Person::getName)
				.containsExactlyInAnyOrder("Lana Wachowski", "Lilly Wachowski", "Tom Tykwer");
		};

		assertThat(movies)
			.hasSize(NUMBER_OF_INITIAL_MOVIES)
			.filteredOn(Movie::getTitle, "Cloud Atlas")
			.singleElement()
			.satisfies(theMovieCloudAtlas);
	}

	@SpringBootApplication
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
				.baseUrl("http://%s/api".formatted(hostUnderTest))
				.filter(documentationConfiguration(restDocumentation))
				.responseTimeout(Duration.ofSeconds(30)) // Hopefully high enough that the container under tests are up
				.codecs(c -> {
					c.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
					c.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
				})
				.build();
		}
	}

	private static List<String> readStatements(Resource script) {

		List<String> newStatements = new ArrayList<>();
		try (var scanner = new Scanner(script.getInputStream(), StandardCharsets.UTF_8).useDelimiter(";\r?\n")) {
			while (scanner.hasNext()) {
				String statement = scanner.next().trim().replaceAll(";$", "").trim();
				if (statement.isEmpty()) {
					continue;
				}
				newStatements.add(statement);
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		return Collections.unmodifiableList(newStatements);
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
