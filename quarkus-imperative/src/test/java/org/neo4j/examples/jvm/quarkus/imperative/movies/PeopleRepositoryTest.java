package org.neo4j.examples.jvm.quarkus.imperative.movies;

import static org.assertj.core.api.Assertions.assertThat;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.junit.QuarkusTest;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(value = EmbeddedNeo4jTestResource.class, initArgs = {
	@ResourceArg(name = "fixture", value = PeopleRepositoryTest.FIXTURE) })
public class PeopleRepositoryTest {

	@Inject
	PeopleRepository peopleRepository;

	@Test
	void getDetailsByNameShouldWork() {

		var optionalDetails = peopleRepository.getDetailsByName("Keanu Reeves");

		assertThat(optionalDetails).hasValueSatisfying(personDetails -> {
			assertThat(personDetails.getName()).isEqualTo("Keanu Reeves");
			assertThat(personDetails.getBorn()).isEqualTo(1964);
			assertThat(personDetails.getActedIn())
				.hasSize(3)
				.extracting(Movie::getTitle).contains("The Matrix Reloaded");
			assertThat(personDetails.getRelated()).hasSize(5);
		});

		assertThat(peopleRepository.getDetailsByName("foobar")).isEmpty();
	}

	static final String FIXTURE = ""
								  + "CREATE (TheMatrix:Movie {title:'The Matrix', released:1999, tagline:'Welcome to the Real World'})\n"
								  + "CREATE (Keanu:Person {name:'Keanu Reeves', born:1964})\n"
								  + "CREATE (Carrie:Person {name:'Carrie-Anne Moss', born:1967})\n"
								  + "CREATE (Laurence:Person {name:'Laurence Fishburne', born:1961})\n"
								  + "CREATE (Hugo:Person {name:'Hugo Weaving', born:1960})\n"
								  + "CREATE (LillyW:Person {name:'Lilly Wachowski', born:1967})\n"
								  + "CREATE (LanaW:Person {name:'Lana Wachowski', born:1965})\n"
								  + "CREATE (JoelS:Person {name:'Joel Silver', born:1952})\n"
								  + "CREATE (KevinB:Person {name:'Kevin Bacon', born:1958})\n"
								  + "CREATE\n"
								  + "(Keanu)-[:ACTED_IN {roles:['Neo']}]->(TheMatrix),\n"
								  + "(Carrie)-[:ACTED_IN {roles:['Trinity']}]->(TheMatrix),\n"
								  + "(Laurence)-[:ACTED_IN {roles:['Morpheus']}]->(TheMatrix),\n"
								  + "(Hugo)-[:ACTED_IN {roles:['Agent Smith']}]->(TheMatrix),\n"
								  + "(LillyW)-[:DIRECTED]->(TheMatrix),\n"
								  + "(LanaW)-[:DIRECTED]->(TheMatrix),\n"
								  + "(JoelS)-[:PRODUCED]->(TheMatrix)\n"
								  + "\n"
								  + "CREATE (Emil:Person {name:\"Emil Eifrem\", born:1978})\n"
								  + "CREATE (Emil)-[:ACTED_IN {roles:[\"Emil\"]}]->(TheMatrix)\n"
								  + "\n"
								  + "CREATE (TheMatrixReloaded:Movie {title:'The Matrix Reloaded', released:2003, tagline:'Free your mind'})\n"
								  + "CREATE\n"
								  + "(Keanu)-[:ACTED_IN {roles:['Neo']}]->(TheMatrixReloaded),\n"
								  + "(Carrie)-[:ACTED_IN {roles:['Trinity']}]->(TheMatrixReloaded),\n"
								  + "(Laurence)-[:ACTED_IN {roles:['Morpheus']}]->(TheMatrixReloaded),\n"
								  + "(Hugo)-[:ACTED_IN {roles:['Agent Smith']}]->(TheMatrixReloaded),\n"
								  + "(LillyW)-[:DIRECTED]->(TheMatrixReloaded),\n"
								  + "(LanaW)-[:DIRECTED]->(TheMatrixReloaded),\n"
								  + "(JoelS)-[:PRODUCED]->(TheMatrixReloaded)\n"
								  + "\n"
								  + "CREATE (TheMatrixRevolutions:Movie {title:'The Matrix Revolutions', released:2003, tagline:'Everything that has a beginning has an end'})\n"
								  + "CREATE\n"
								  + "(Keanu)-[:ACTED_IN {roles:['Neo']}]->(TheMatrixRevolutions),\n"
								  + "(Carrie)-[:ACTED_IN {roles:['Trinity']}]->(TheMatrixRevolutions),\n"
								  + "(Laurence)-[:ACTED_IN {roles:['Morpheus']}]->(TheMatrixRevolutions),\n"
								  + "(KevinB)-[:ACTED_IN {roles:['Unknown']}]->(TheMatrixRevolutions),\n"
								  + "(Hugo)-[:ACTED_IN {roles:['Agent Smith']}]->(TheMatrixRevolutions),\n"
								  + "(LillyW)-[:DIRECTED]->(TheMatrixRevolutions),\n"
								  + "(LanaW)-[:DIRECTED]->(TheMatrixRevolutions),\n"
								  + "(JoelS)-[:PRODUCED]->(TheMatrixRevolutions)\n";
}