package org.neo4j.examples.jvm.micronaut.reactive.movies;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.util.Map;

import javax.inject.Singleton;

import org.neo4j.driver.Driver;
import org.neo4j.driver.types.Node;

/**
 * @author Michael J. Simons
 */
@Singleton
public class PeopleRepository {

	private final Driver driver;

	PeopleRepository(Driver driver) {
		this.driver = driver;
	}

	public Single<Person> save(Person person) {

		var query = ""
					+ "MERGE (p:Person {name: $name})\n"
					+ "SET p.born = $born\n"
					+ "RETURN p\n";
		var parameters = Map.<String, Object>of("name", person.getName(), "born", person.getBorn());

		return Single.using(
			driver::rxSession,
			session -> Flowable.fromPublisher(session.writeTransaction(tx -> tx.run(query, parameters).records()))
				.map(r -> asPerson(r.get("p").asNode()))
				.singleOrError(),
			session -> Observable.fromPublisher(session.close()).subscribe()
		);
	}

	static Person asPerson(Node personNode) {
		return new Person(personNode.id(), personNode.get("name").asString(), personNode.get("born").asInt());
	}
}
