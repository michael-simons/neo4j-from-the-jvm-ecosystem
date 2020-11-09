package org.neo4j.examples.jvm.tck;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.Driver;
import org.neo4j.examples.jvm.tck.movies.Actor;
import org.neo4j.examples.jvm.tck.movies.Movie;
import org.neo4j.examples.jvm.tck.movies.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Michael J. Simons
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class Tck {

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

	@BeforeAll
	static void prepareDatabase(@Autowired Driver driver, @Value("classpath:movies.cypher") Resource movies)
		throws IOException {

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
		WebTestClient webTestClient(ObjectMapper objectMapper) {

			return WebTestClient
				.bindToServer()
				.baseUrl("http://localhost:8080/api")
				.codecs(c -> {
					c.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper));
					c.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper));
				})
				.build();
		}
	}
}
