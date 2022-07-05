package org.neo4j.examples.jvm.spring.data.imperative.movies;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.examples.jvm.spring.data.imperative.support.Neo4jOGMConfig;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * @author Michael J. Simons
 */
@DataNeo4jTest(includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = Neo4jOGMConfig.class))
public class MovieRepositoryTest {
	private static Neo4j embeddedDatabaseServer;

	@DynamicPropertySource
	static void neo4jProperties(DynamicPropertyRegistry registry) {

		registry.add("spring.neo4j.uri", embeddedDatabaseServer::boltURI);
		registry.add("spring.neo4j.authentication.username", () -> "neo4j");
		registry.add("spring.neo4j.authentication.password", () -> null);
	}

	@Test
	public void findByDerivedQueryhouldWork(@Autowired MovieRepository movieRepository) {
		assertThat(movieRepository.findByTitle("foobar")).isEmpty();
		assertThat(movieRepository.findByTitle("The Matrix")).map(Movie::getDescription).hasValue("Welcome to the Real World");
	}

	@BeforeAll
	static void initializeNeo4j() {

		embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
			.withDisabledServer()
			.withFixture("""
				CREATE (TheMatrix:Movie {title:'The Matrix', released:1999, tagline:'Welcome to the Real World'})
				"""
			)
			.build();
	}

	@AfterAll
	static void stopNeo4j() {

		embeddedDatabaseServer.close();
	}
}
