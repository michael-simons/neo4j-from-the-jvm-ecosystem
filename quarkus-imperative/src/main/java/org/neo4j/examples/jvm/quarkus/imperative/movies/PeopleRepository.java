package org.neo4j.examples.jvm.quarkus.imperative.movies;

import static org.neo4j.examples.jvm.quarkus.imperative.movies.MovieRepository.asMovie;

import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.neo4j.driver.Driver;
import org.neo4j.driver.types.Node;

/**
 * @author Michael J. Simons
 */
@ApplicationScoped
class PeopleRepository {

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

	Optional<PersonDetails> getDetailsByName(String name) {
		try (var session = driver.session()) {

			var query = ""
				  + "MATCH (person:Person {name: $name})\n"
				  + "OPTIONAL MATCH (person)-[:DIRECTED]->(d:Movie)\n"
				  + "OPTIONAL MATCH (person)<-[r:ACTED_IN]->(a:Movie)\n"
				  + "OPTIONAL MATCH (person)-->(movies)<-[relatedRole:ACTED_IN]-(relatedPerson)\n"
				  + "RETURN DISTINCT person,\n"
				  + "collect(DISTINCT d) AS directed,\n"
				  + "collect(DISTINCT a) AS actedIn,\n"
				  + "collect(DISTINCT relatedPerson) AS related\n";

			return session.readTransaction(tx -> {
				var result = tx.run(query, Map.of("name", name));
				if (!result.hasNext()) {
					return Optional.empty();
				}

				var record = result.single();
				var person = asPerson(record.get("person").asNode());
				var directed = record.get("directed").asList(v -> asMovie(v.asNode()));
				var actedIn = record.get("actedIn").asList(v -> asMovie(v.asNode()));
				var related = record.get("related").asList(v -> asPerson(v.asNode()));

				return Optional.of(new PersonDetails(person.getName(), person.getBorn(), actedIn, directed, related));
			});
		}
	}

	static Person asPerson(Node personNode) {
		return new Person(personNode.id(), personNode.get("name").asString(), personNode.get("born").asInt());
	}
}
