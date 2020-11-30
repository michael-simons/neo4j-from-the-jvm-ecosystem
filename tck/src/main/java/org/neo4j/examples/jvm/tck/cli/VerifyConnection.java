package org.neo4j.examples.jvm.tck.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.neo4j.driver.Driver;
import org.neo4j.driver.exceptions.ServiceUnavailableException;

/**
 * @author Michael J. Simons
 */
@Command(name = "verifyConnection", description = "Tries to open up a connection to the database.")
final class VerifyConnection implements Callable<Integer> {

	@Option(
		names = { "-t", "--timeout" },
		description = "The timeout for the verification.",
		required = true,
		defaultValue = "PT120S"
	)
	private final Duration timeout = Duration.ofSeconds(120);

	private final Driver driver;

	VerifyConnection(Driver driver) {
		this.driver = driver;
	}

	@Override
	public Integer call() throws Exception {

		var waitingStarted = Instant.now();
		while (Duration.between(waitingStarted, Instant.now()).compareTo(timeout) < 0) {
			try {
				driver.verifyConnectivity();
				return CommandLine.ExitCode.OK;
			} catch (ServiceUnavailableException e) {
				Cli.LOGGER.log(Level.INFO, "Waiting for Neo4j to be reachable.");
				Thread.sleep(500);
			}
		}

		return CommandLine.ExitCode.SOFTWARE;
	}
}
