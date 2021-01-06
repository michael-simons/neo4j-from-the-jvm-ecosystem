package org.neo4j.examples.jvm.vertx.sdn.movies;

import reactor.core.publisher.Mono;

import org.springframework.data.neo4j.repository.ReactiveNeo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

/**
 * @author Michael J. Simons
 */
public interface PeopleRepository extends ReactiveNeo4jRepository<Person, Long> {

	@Query("""
		MATCH (person:Person {name: $name})
		OPTIONAL MATCH (person)-[:DIRECTED]->(d:Movie)
		OPTIONAL MATCH (person)<-[r:ACTED_IN]->(a:Movie)
		OPTIONAL MATCH (person)-->(movies)<-[relatedRole:ACTED_IN]-(relatedPerson)		
		RETURN DISTINCT person,
		collect(DISTINCT d) AS directed,
		collect(DISTINCT a) AS actedIn,
		collect(DISTINCT relatedPerson) AS related
		""")
	Mono<PersonDetails> getDetailsByName(String name);
}
