package org.neo4j.examples.jvm.tck.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

import java.util.logging.Logger;

@Command(
	name = "tck",
	mixinStandardHelpOptions = true,
	description = "Provides a set of helpful commands against a Neo4j database",
	subcommands = { LoadMovies.class, VerifyConnection.class }
)
public final class Cli implements Runnable {

	static final Logger LOGGER = Logger.getLogger(Cli.class.getName());

	@Spec
	private CommandSpec commandSpec;

	@Override
	public void run() {
		throw new ParameterException(commandSpec.commandLine(), "Missing required subcommand");
	}
}
