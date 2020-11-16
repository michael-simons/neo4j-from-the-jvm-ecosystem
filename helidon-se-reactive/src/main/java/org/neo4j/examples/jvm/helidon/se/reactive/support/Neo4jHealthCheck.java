package org.neo4j.examples.jvm.helidon.se.reactive.support;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

/**
 * @author Michael J. Simons
 */
public class Neo4jHealthCheck implements HealthCheck {

	/**
	 * The Cypher statement used to verify Neo4j is up.
	 */
	static final String CYPHER = "CALL dbms.components() YIELD name, edition WHERE name = 'Neo4j Kernel' RETURN edition";

	private final Driver driver;

	public Neo4jHealthCheck(Driver driver) {
		this.driver = driver;
	}

	private HealthCheckResponse runHealthCheckQuery() {

		// We use WRITE here to make sure UP is returned for a server that supports
		// all possible workloads.
		// We also use a transactional function so that we retry this in case the cluster
		// is down.
		try (Session session = this.driver.session()) {

			return session.writeTransaction(tx -> {
				var result = tx.run(CYPHER);

				var edition = result.single().get("edition").asString();
				var resultSummary = result.consume();
				var serverInfo = resultSummary.server();

				var responseBuilder = HealthCheckResponse.named("neo4j")
					.withData("server", serverInfo.version() + "@" + serverInfo.address())
					.withData("edition", edition);

				var databaseInfo = resultSummary.database();
				if (!databaseInfo.name().trim().isBlank()) {
					responseBuilder.withData("database", databaseInfo.name().trim());
				}

				return responseBuilder.up().build();
			});
		}
	}

	@Override
	public HealthCheckResponse call() {

		try {
			return runHealthCheckQuery();
		} catch (Exception ex) {
			return HealthCheckResponse.down("neo4j");
		}
	}
}
