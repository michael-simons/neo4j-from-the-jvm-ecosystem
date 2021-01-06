package org.neo4j.examples.jvm.vertx.sdn;

import io.vertx.core.Vertx;

import org.neo4j.driver.Driver;
import org.neo4j.examples.jvm.vertx.sdn.movies.MovieRepository;
import org.neo4j.examples.jvm.vertx.sdn.movies.PeopleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * This kicks off VertX.
 */
@Component
public final class VertxRunner implements CommandLineRunner {

	private final int port;

	private final Driver driver;
	private final MovieRepository movieService;
	private final PeopleRepository peopleRepository;

	public VertxRunner(@Value("${server.port:8080}") int port,
		Driver driver, MovieRepository movieService,
		PeopleRepository peopleRepository
	) {
		this.port = port;
		this.driver = driver;

		this.movieService = movieService;
		this.peopleRepository = peopleRepository;
	}

	@Override
	public void run(String... args) {

		final Vertx vertx = Vertx.vertx();
		vertx.deployVerticle(new APIVerticle(port, driver, movieService, peopleRepository));
	}
}
