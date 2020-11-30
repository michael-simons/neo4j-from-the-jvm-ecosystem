package org.neo4j.examples.jvm.spring.plain.reactive.movies;

import reactor.core.publisher.Flux;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Michael J. Simons
 */
@RestController
@RequestMapping("/api/movies")
public final class MoviesController {

	private final MovieRepository movieRepository;

	MoviesController(MovieRepository movieRepository) {
		this.movieRepository = movieRepository;
	}

	@GetMapping(value = { "", "/" }, produces = MediaType.APPLICATION_JSON_VALUE)
	public Flux<Movie> get() {
		return movieRepository.findAll();
	}
}
