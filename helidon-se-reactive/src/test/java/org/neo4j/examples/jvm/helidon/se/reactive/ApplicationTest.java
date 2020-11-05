
package org.neo4j.examples.jvm.helidon.se.reactive;

import io.helidon.webserver.WebServer;

import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonBuilderFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.GraphDatabase;
import org.testcontainers.containers.Neo4jContainer;

/**
 * @author Michael J. Simons
 */
class ApplicationTest {

	private static final JsonBuilderFactory JSON = Json.createBuilderFactory(Map.of());

	private static Neo4jContainer neo4jContainer;

	private static WebServer webServer;

	@BeforeAll
	static void startTheServer() throws Exception {

		neo4jContainer = new Neo4jContainer<>("neo4j:4.0")
			.withAdminPassword("secret");
		neo4jContainer.start();

		try (var driver = GraphDatabase.driver(neo4jContainer.getBoltUrl(), AuthTokens.basic("neo4j", "secret"));
			var session = driver.session()
		) {
			session.writeTransaction(tx -> tx.run(""
												  + "CREATE (TheMatrix:Movie {title:'The Matrix', released:1999, tagline:'Welcome to the Real World'})\n"
												  + "CREATE (Keanu:Person {name:'Keanu Reeves', born:1964})\n"
												  + "CREATE (Carrie:Person {name:'Carrie-Anne Moss', born:1967})\n"
												  + "CREATE (Laurence:Person {name:'Laurence Fishburne', born:1961})\n"
												  + "CREATE (Hugo:Person {name:'Hugo Weaving', born:1960})\n"
												  + "CREATE (LillyW:Person {name:'Lilly Wachowski', born:1967})\n"
												  + "CREATE (LanaW:Person {name:'Lana Wachowski', born:1965})\n"
												  + "CREATE (JoelS:Person {name:'Joel Silver', born:1952})\n"
												  + "CREATE\n"
												  + "  (Keanu)-[:ACTED_IN {roles:['Neo']}]->(TheMatrix),\n"
												  + "  (Carrie)-[:ACTED_IN {roles:['Trinity']}]->(TheMatrix),\n"
												  + "  (Laurence)-[:ACTED_IN {roles:['Morpheus']}]->(TheMatrix),\n"
												  + "  (Hugo)-[:ACTED_IN {roles:['Agent Smith']}]->(TheMatrix),\n"
												  + "  (LillyW)-[:DIRECTED]->(TheMatrix),\n"
												  + "  (LanaW)-[:DIRECTED]->(TheMatrix),\n"
												  + "  (JoelS)-[:PRODUCED]->(TheMatrix)").consume()
			);
		}

		// Don't know how to set this dynamically otherwise in Helidon
		System.setProperty("neo4j.driver.uri", neo4jContainer.getBoltUrl());

		webServer = Application.startServer();

		long timeout = 2000; // 2 seconds should be enough to start the server
		long now = System.currentTimeMillis();

		while (!webServer.isRunning()) {
			Thread.sleep(100);
			if ((System.currentTimeMillis() - now) > timeout) {
				Assertions.fail("Failed to start webserver");
			}
		}
	}

	@AfterAll
	static void stopServer() throws Exception {
		if (webServer != null) {
			webServer.shutdown()
				.toCompletableFuture()
				.get(10, TimeUnit.SECONDS);
		}
	}

	@AfterAll
	public static void stopNeo4j() {
		neo4jContainer.stop();
	}

	@Test
	void getMoviesShouldWork() throws Exception {

		var client = HttpClient.newBuilder()
			.build();

		var request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + webServer.port() + "/api/movies"))
			.GET().build();

		var response = client.send(request, HttpResponse.BodyHandlers.ofString());

		var resultObject = Json.createReader(new StringReader(response.body().replaceFirst("data: ", ""))).readObject();
		Assertions.assertEquals("The Matrix", resultObject.getString("title"));
	}
}
