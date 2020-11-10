package org.neo4j.examples.jvm.spring.plain.reactive.movies;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

import org.neo4j.driver.Driver;
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

	static Person asPerson(Node personNode) {
		return new Person(personNode.id(), personNode.get("name").asString(), personNode.get("born").asInt());
	}
}
