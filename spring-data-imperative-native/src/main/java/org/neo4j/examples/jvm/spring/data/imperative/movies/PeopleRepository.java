package org.neo4j.examples.jvm.spring.data.imperative.movies;

import java.util.Optional;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

/**
 * @author Michael J. Simons
 */
interface PeopleRepository extends Neo4jRepository<Person, Long> {

	@Query(""
		   + "MATCH (person:Person)\n"
		   + "WHERE id(person) = $id\n"
		   + "WITH person\n"
		   + "OPTIONAL MATCH (person)-[:DIRECTED]->(d:Movie)\n"
		   + "OPTIONAL MATCH (person)<-[r:ACTED_IN]->(a:Movie)\n"
		   + "OPTIONAL MATCH (person)-->(movies)<-[relatedRole:ACTED_IN]-(relatedPerson)\n"
		   + "RETURN DISTINCT person,\n"
		   + "collect(DISTINCT d) AS directed,\n"
		   + "collect(DISTINCT a) AS actedIn,\n"
		   + "collect(DISTINCT relatedPerson) AS related\n")
	Optional<PersonDetails> findDetailsById(Long id);
}
