package org.neo4j.examples.jvm.quarkus.reactive.movies;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;

/**
 * @author Michael J. Simons
 */
public final class EmbeddedNeo4jTestResource implements QuarkusTestResourceLifecycleManager {

	private String fixture;
	private Neo4j embeddedDatabaseServer;

	@Override
	public void init(Map<String, String> initArgs) {
		this.fixture = initArgs.getOrDefault("fixture", "");
	}

	@Override
	public Map<String, String> start() {

		this.embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
			.withDisabledServer()
			.withFixture(fixture)
			.build();

		return Map.of(
			"quarkus.neo4j.uri", this.embeddedDatabaseServer.boltURI().toString(),
			"quarkus.neo4j.authentication.username", "neo4j",
			"quarkus.neo4j.authentication.password", "n/a"
		);
	}

	@Override
	public void stop() {

		this.embeddedDatabaseServer.close();
	}
}
