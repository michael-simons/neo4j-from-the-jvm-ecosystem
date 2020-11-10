package org.neo4j.examples.jvm.quarkus.imperative.movies;

import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.neo4j.driver.Driver;
import org.neo4j.driver.types.Node;

/**
 * @author Michael J. Simons
 */
@ApplicationScoped
public class PeopleRepository {

	private final Driver driver;

	PeopleRepository(Driver driver) {
		this.driver = driver;
	}

	Person save(Person person) {

		try (var session = driver.session()) {

			var query = ""
						+ "MERGE (p:Person {name: $name})\n"
						+ "SET p.born = $born\n"
						+ "RETURN p\n";

			var personNode = session
				.writeTransaction(tx -> tx.run(
					query, Map.of("name", person.getName(), "born", person.getBorn())).single())
				.get("p").asNode();

			return asPerson(personNode);
		}
	}

	static Person asPerson(Node personNode) {
		return new Person(personNode.id(), personNode.get("name").asString(), personNode.get("born").asInt());
	}
}
