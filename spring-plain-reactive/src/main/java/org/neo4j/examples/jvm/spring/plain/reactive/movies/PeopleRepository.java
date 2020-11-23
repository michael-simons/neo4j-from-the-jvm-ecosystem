package org.neo4j.examples.jvm.spring.plain.reactive.movies;

import static org.neo4j.examples.jvm.spring.plain.reactive.movies.MovieRepository.asMovie;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Value;
import org.neo4j.driver.reactive.RxSession;
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

	Mono<Person> save(Person person) {

		var query = """
			MERGE (p:Person {name: $name})
			SET p.born = $born
			RETURN p
			""";
		var parameters = Map.<String, Object>of("name", person.getName(), "born", person.getBorn());

		return Flux.using(driver::rxSession,
			session -> session.writeTransaction(tx -> tx.run(query, parameters).records()),
			RxSession::close
		).single().map(r -> asPerson(r.get("p").asNode()));
	}

	Mono<PersonDetails> getDetailsByName(String name) {

		var query = """
				MATCH (person:Person {name: $name})
				OPTIONAL MATCH (person)-[:DIRECTED]->(d:Movie)
				OPTIONAL MATCH (person)<-[r:ACTED_IN]->(a:Movie)
				OPTIONAL MATCH (person)-->(movies)<-[relatedRole:ACTED_IN]-(relatedPerson)		
				RETURN DISTINCT person,
				collect(DISTINCT d) AS directed,
				collect(DISTINCT a) AS actedIn,
				collect(DISTINCT relatedPerson) AS related
				""";

		return Mono.using(
			driver::rxSession,
			rxSession -> Mono.from(rxSession.readTransaction(tx -> tx.run(query, Map.of("name", name)).records())).map(record -> {
				var person = asPerson(record.get("person").asNode());
				var directed = record.get("directed").asList(v -> asMovie(v.asNode()));
				var actedIn = record.get("actedIn").asList(v -> asMovie(v.asNode()));
				var related = record.get("related").asList(v -> asPerson(v.asNode()));

				return new PersonDetails(person.getName(), person.getBorn(), actedIn, directed, related);
			}),
			RxSession::close
		);
	}

	static Person asPerson(Node personNode) {
		return new Person(personNode.id(), personNode.get("name").asString(), personNode.get("born").asInt());
	}
}
