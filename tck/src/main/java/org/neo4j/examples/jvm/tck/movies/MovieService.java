package org.neo4j.examples.jvm.tck.movies;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * @author Michael J. Simons
 */
@Service
public final class MovieService {

	private final Driver driver;

	private final Resource movies;

	public MovieService(Driver driver, @Value("classpath:movies.cypher") Resource movies) {
		this.driver = driver;
		this.movies = movies;
	}

	/**
	 * Deletes the databases content and loads the movie graph.
	 */
	public void prepareDatabase() {

		try (
			var session = driver.session();
			var tx = session.beginTransaction()
		) {
			tx.run("MATCH (n) DETACH DELETE n");
			readStatements(movies).forEach(tx::run);
			tx.commit();
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
}
