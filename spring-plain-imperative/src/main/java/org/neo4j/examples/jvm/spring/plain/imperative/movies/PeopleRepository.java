package org.neo4j.examples.jvm.spring.plain.imperative.movies;

import java.util.Map;

import org.neo4j.driver.Driver;
import org.neo4j.driver.types.Node;
import org.springframework.stereotype.Repository;

/**
 * @author Michael J. Simons
 */
@Repository
final class PeopleRepository {

	private final Driver driver;

	PeopleRepository(Driver driver) {
		this.driver = driver;
	}

	Person save(Person person) {

		try (var session = driver.session()) {

			var query = """
				MERGE (p:Person {name: $name})
				SET p.born = $born
				RETURN p
				""";

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
