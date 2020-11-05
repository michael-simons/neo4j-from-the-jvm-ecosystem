package org.neo4j.examples.jvm.spring.data.imperative.movies;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;

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

	@GetMapping({ "", "/" })
	public List<Movie> get() {
		return movieRepository.findAll();
	}

	@Autowired
	PeopleRepository peopleRepository;

	@GetMapping("/f")
	public PersonDetails g() {
		return peopleRepository.findDetailsById(166L).orElseThrow(() -> new RuntimeException("not found"));
	}
}
