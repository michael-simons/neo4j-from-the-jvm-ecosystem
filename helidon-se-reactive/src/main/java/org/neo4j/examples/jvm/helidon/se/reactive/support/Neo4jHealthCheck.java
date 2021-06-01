package org.neo4j.examples.jvm.helidon.se.reactive.support;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;
import org.eclipse.microprofile.health.Readiness;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;

/**
 * @author Michael J. Simons
 */
@Readiness
public class Neo4jHealthCheck implements HealthCheck {

	/**
	 * The Cypher statement used to verify Neo4j is up.
	 */
	static final String CYPHER = "CALL dbms.components() YIELD name, edition WHERE name = 'Neo4j Kernel' RETURN edition";

	private final Driver driver;

	Neo4jHealthCheck(Driver driver) {
		this.driver = driver;
	}

	public static Neo4jHealthCheck create(Driver driver) {
		return new Neo4jHealthCheck(driver);
	}

	private HealthCheckResponse runHealthCheckQuery(HealthCheckResponseBuilder builder) {

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

				var responseBuilder = builder
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

		var builder = HealthCheckResponse.named("Neo4j connection health check");
		try {
			return runHealthCheckQuery(builder);
		} catch (Exception ex) {
			return builder.down().withData("reason", ex.getMessage()).build();
		}
	}
}
