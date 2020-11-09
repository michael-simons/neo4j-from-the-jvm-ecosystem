package org.neo4j.examples.jvm.micronaut.reactive.movies;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.reactivex.Flowable;

/**
 * @author Michael J. Simons
 */
@Controller("/api/movies")
public final class MoviesController {

	private final MovieRepository movieRepository;

	MoviesController(MovieRepository movieRepository) {
		this.movieRepository = movieRepository;
	}

	@Get(uris = { "", "/" }, produces = MediaType.TEXT_EVENT_STREAM)
	public Flowable<Movie> get() {
		return movieRepository.findAll();
	}
}
