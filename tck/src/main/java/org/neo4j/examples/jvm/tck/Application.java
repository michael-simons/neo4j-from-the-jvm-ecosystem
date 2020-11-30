package org.neo4j.examples.jvm.tck;

import picocli.CommandLine;

import org.neo4j.examples.jvm.tck.cli.Cli;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Michael J. Simons
 */
@SpringBootApplication
public class Application implements CommandLineRunner, ExitCodeGenerator {

	public static void main(String[] args) {
		System.exit(SpringApplication.exit(SpringApplication.run(Application.class, args)));
	}

	private final CommandLine.IFactory factory;

	private int exitCode;

	Application(CommandLine.IFactory factory) {
		this.factory = factory;
	}

	@Override
	public void run(String... args) {
		this.exitCode = new CommandLine(Cli.class, factory)
			.setUnmatchedArgumentsAllowed(true)
			.execute(args);
	}

	@Override
	public int getExitCode() {
		return this.exitCode;
	}
}
