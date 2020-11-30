package org.neo4j.examples.jvm.micronaut.imperative.movies;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

import java.util.List;

/**
 * @author Michael J. Simons
 */
@Controller("/api/movies")
public final class MoviesController {

	private final MovieRepository movieRepository;

	MoviesController(MovieRepository movieRepository) {
		this.movieRepository = movieRepository;
	}

	@Get(uris = { "", "/" }, produces = MediaType.APPLICATION_JSON)
	public List<Movie> get() {
		return movieRepository.findAll();
	}
}
