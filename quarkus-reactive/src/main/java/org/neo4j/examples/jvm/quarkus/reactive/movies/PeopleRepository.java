package org.neo4j.examples.jvm.quarkus.reactive.movies;

import static org.neo4j.examples.jvm.quarkus.reactive.movies.MovieRepository.asMovie;

import io.smallrye.mutiny.Uni;

import java.time.Duration;
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
class PeopleRepository {

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
			.flatMap(session -> Uni.createFrom()
				.publisher(session.writeTransaction(tx -> tx.run(query, parameters).records())))
			.map(r -> asPerson(r.get("p").asNode()))
			.onTermination().invoke(() -> Uni.createFrom().publisher(sessionHolder.get().close()).subscribe());
	}

	Uni<PersonDetails> getDetailsByName(String name) {

		var query = ""
			+ "MATCH (person:Person {name: $name})\n"
			+ "OPTIONAL MATCH (person)-[:DIRECTED]->(d:Movie)\n"
			+ "OPTIONAL MATCH (person)<-[r:ACTED_IN]->(a:Movie)\n"
			+ "OPTIONAL MATCH (person)-->(movies)<-[relatedRole:ACTED_IN]-(relatedPerson)\n"
			+ "RETURN DISTINCT person,\n"
			+ "collect(DISTINCT d) AS directed,\n"
			+ "collect(DISTINCT a) AS actedIn,\n"
			+ "collect(DISTINCT relatedPerson) AS related\n";


		var sessionHolder = new AtomicReference<RxSession>();
		return Uni.createFrom()
			.deferred(() -> {
				var session = driver.rxSession();
				sessionHolder.set(session);
				return Uni.createFrom().item(session);
			})
			.flatMap(session -> Uni.createFrom().publisher(session.readTransaction(tx -> tx.run(query, Map.of("name", name)).records())))
			.onItem().ifNotNull().transform(record -> {

				var person = asPerson(record.get("person").asNode());
				var directed = record.get("directed").asList(v -> asMovie(v.asNode()));
				var actedIn = record.get("actedIn").asList(v -> asMovie(v.asNode()));
				var related = record.get("related").asList(v -> asPerson(v.asNode()));

				return new PersonDetails(person.getName(), person.getBorn(), actedIn, directed, related);
			})
			.onTermination().invoke(() -> Uni.createFrom().publisher(sessionHolder.get().close()).subscribe());
	}

	static Person asPerson(Node personNode) {
		return new Person(personNode.id(), personNode.get("name").asString(), personNode.get("born").asInt());
	}
}
