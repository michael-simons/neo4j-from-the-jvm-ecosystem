package org.neo4j.examples.jvm.helidon.se.reactive.movies;

import static org.reactivestreams.FlowAdapters.toFlowPublisher;

import io.helidon.common.reactive.Single;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.neo4j.driver.Driver;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.driver.types.Node;

/**
 * @author Michael J. Simons
 */
public final class PeopleRepository {

	private final Driver driver;

	public PeopleRepository(Driver driver) {
		this.driver = driver;
	}

	public Single<Person> save(Person person) {

		var query = ""
					+ "MERGE (p:Person {name: $name})\n"
					+ "SET p.born = $born\n"
					+ "RETURN p\n";
		var parameters = Map.<String, Object>of("name", person.getName(), "born", person.getBorn());

		var sessionHolder = new AtomicReference<RxSession>();
		return Single
			.defer(() -> {
				var session = driver.rxSession();
				sessionHolder.set(session);
				return Single.just(session);
			})
			.flatMapSingle(session -> Single.create(
				toFlowPublisher(session.writeTransaction(tx -> tx.run(query, parameters).records()))))
			.map(r -> asPerson(r.get("p").asNode()))
			.onTerminate(() -> Single.create(toFlowPublisher(sessionHolder.get().close()))
				.toOptionalSingle()
				.subscribe(empty -> {
				}));
	}

	static Person asPerson(Node personNode) {
		return new Person(personNode.id(), personNode.get("name").asString(), personNode.get("born").asInt());
	}
}
