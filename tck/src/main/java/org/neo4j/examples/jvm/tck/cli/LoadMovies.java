package org.neo4j.examples.jvm.tck.cli;

import ac.simons.neo4j.migrations.core.Migrations;
import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * @author Michael J. Simons
 */
@Command(name = "loadMovies", description = "Clears the database and loads the movie graph.")
final class LoadMovies implements Callable<Integer> {


	private final Migrations migrations;

	LoadMovies(Migrations movieService) {
		this.migrations = movieService;
	}

	@Override
	public Integer call() {

		try {
			this.migrations.apply().ifPresent(v -> Cli.LOGGER.info("Database now at " + v.getValue()));
			return CommandLine.ExitCode.OK;
		} catch (Exception e) {
			Cli.LOGGER.severe("Could not apply migrations: " + e.getMessage());
			return CommandLine.ExitCode.SOFTWARE;
		}
	}
}
