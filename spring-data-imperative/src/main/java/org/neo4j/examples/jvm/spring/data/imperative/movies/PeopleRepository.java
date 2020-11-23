package org.neo4j.examples.jvm.spring.data.imperative.movies;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

/**
 * @author Michael J. Simons
 */
interface PeopleRepository extends Neo4jRepository<Person, Long> {

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
	Optional<PersonDetails> getDetailsByName(String name);
}
