package org.neo4j.examples.jvm.quarkus.reactive.movies;

import io.smallrye.mutiny.Uni;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.context.ApplicationScoped;

import org.neo4j.driver.Driver;
import org.neo4j.driver.reactive.RxSession;
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

	Uni<Person> save(Person person) {

		var query = ""
					+ "MERGE (p:Person {name: $name})\n"
					+ "SET p.born = $born\n"
					+ "RETURN p\n";
		var parameters = Map.<String, Object>of("name", person.getName(), "born", person.getBorn());

		var sessionHolder = new AtomicReference<RxSession>();
		return Uni.createFrom()
			.deferred(() -> {
				var session = driver.rxSession();
				sessionHolder.set(session);
				return Uni.createFrom().item(session);
			})
			.flatMap(session -> Uni.createFrom().publisher(session.writeTransaction(tx -> tx.run(query, parameters).records())))
			.map(r -> asPerson(r.get("p").asNode()))
			.onTermination().invoke(() -> Uni.createFrom().publisher(sessionHolder.get().close()).subscribe());
	}

	static Person asPerson(Node personNode) {
		return new Person(personNode.id(), personNode.get("name").asString(), personNode.get("born").asInt());
	}
}
