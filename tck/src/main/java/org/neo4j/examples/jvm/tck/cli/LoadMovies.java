package org.neo4j.examples.jvm.tck.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

import org.neo4j.examples.jvm.tck.movies.MovieService;

/**
 * @author Michael J. Simons
 */
@Command(name = "loadMovies", description = "Clears the database and loads the movie graph.")
final class LoadMovies implements Callable<Integer> {

	private final MovieService movieService;

	LoadMovies(MovieService movieService) {
		this.movieService = movieService;
	}

	@Override
	public Integer call() {

		try {
			this.movieService.prepareDatabase();
			return CommandLine.ExitCode.OK;
		} catch (Exception e) {
			return CommandLine.ExitCode.SOFTWARE;
		}
	}
}
