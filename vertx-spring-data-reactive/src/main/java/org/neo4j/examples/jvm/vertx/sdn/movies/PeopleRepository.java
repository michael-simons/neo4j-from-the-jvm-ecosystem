package org.neo4j.examples.jvm.vertx.sdn.movies;

import io.reactivex.Maybe;

import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.reactive.RxJava2SortingRepository;

/**
 * @author Michael J. Simons
 */
public interface PeopleRepository extends RxJava2SortingRepository<Person, Long> {

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
	Maybe<PersonDetails> getDetailsByName(String name);
}
