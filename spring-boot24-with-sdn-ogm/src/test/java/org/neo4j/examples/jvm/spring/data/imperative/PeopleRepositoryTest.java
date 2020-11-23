package org.neo4j.examples.jvm.spring.data.imperative;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.neo4j.examples.jvm.spring.data.imperative.movies.PeopleRepository;
import org.neo4j.examples.jvm.spring.data.imperative.support.Neo4jOGMConfig;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * @author Michael J. Simons
 */
@DataNeo4jTest(includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = Neo4jOGMConfig.class))
public class PeopleRepositoryTest {
	private static Neo4j embeddedDatabaseServer;

	@DynamicPropertySource
	static void neo4jProperties(DynamicPropertyRegistry registry) {

		registry.add("spring.neo4j.uri", embeddedDatabaseServer::boltURI);
		registry.add("spring.neo4j.authentication.username", () -> "neo4j");
		registry.add("spring.neo4j.authentication.password", () -> null);
	}

	@Test
	public void getDetailsByNameShouldWork(@Autowired PeopleRepository peopleRepository) {

		var optionalDetails = peopleRepository.getDetailsByName("Keanu Reeves");

		assertThat(optionalDetails).hasValueSatisfying(personDetails -> {
			assertThat(personDetails.getActedIn()).hasSize(3);
			assertThat(personDetails.getRelated()).hasSize(5);
		});
	}

	@BeforeAll
	static void initializeNeo4j() {

		embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
			.withDisabledServer()
			.withFixture("""
				CREATE (TheMatrix:Movie {title:'The Matrix', released:1999, tagline:'Welcome to the Real World'})
				CREATE (Keanu:Person {name:'Keanu Reeves', born:1964})
				CREATE (Carrie:Person {name:'Carrie-Anne Moss', born:1967})
				CREATE (Laurence:Person {name:'Laurence Fishburne', born:1961})
				CREATE (Hugo:Person {name:'Hugo Weaving', born:1960})
				CREATE (LillyW:Person {name:'Lilly Wachowski', born:1967})
				CREATE (LanaW:Person {name:'Lana Wachowski', born:1965})
				CREATE (JoelS:Person {name:'Joel Silver', born:1952})
				CREATE (KevinB:Person {name:'Kevin Bacon', born:1958})
				CREATE
				(Keanu)-[:ACTED_IN {roles:['Neo']}]->(TheMatrix),
				(Carrie)-[:ACTED_IN {roles:['Trinity']}]->(TheMatrix),
				(Laurence)-[:ACTED_IN {roles:['Morpheus']}]->(TheMatrix),
				(Hugo)-[:ACTED_IN {roles:['Agent Smith']}]->(TheMatrix),
				(LillyW)-[:DIRECTED]->(TheMatrix),
				(LanaW)-[:DIRECTED]->(TheMatrix),
				(JoelS)-[:PRODUCED]->(TheMatrix)
				    
				CREATE (Emil:Person {name:"Emil Eifrem", born:1978})
				CREATE (Emil)-[:ACTED_IN {roles:["Emil"]}]->(TheMatrix)
				    
				CREATE (TheMatrixReloaded:Movie {title:'The Matrix Reloaded', released:2003, tagline:'Free your mind'})
				CREATE
				(Keanu)-[:ACTED_IN {roles:['Neo']}]->(TheMatrixReloaded),
				(Carrie)-[:ACTED_IN {roles:['Trinity']}]->(TheMatrixReloaded),
				(Laurence)-[:ACTED_IN {roles:['Morpheus']}]->(TheMatrixReloaded),
				(Hugo)-[:ACTED_IN {roles:['Agent Smith']}]->(TheMatrixReloaded),
				(LillyW)-[:DIRECTED]->(TheMatrixReloaded),
				(LanaW)-[:DIRECTED]->(TheMatrixReloaded),
				(JoelS)-[:PRODUCED]->(TheMatrixReloaded)
				    
				CREATE (TheMatrixRevolutions:Movie {title:'The Matrix Revolutions', released:2003, tagline:'Everything that has a beginning has an end'})
				CREATE
				(Keanu)-[:ACTED_IN {roles:['Neo']}]->(TheMatrixRevolutions),
				(Carrie)-[:ACTED_IN {roles:['Trinity']}]->(TheMatrixRevolutions),
				(Laurence)-[:ACTED_IN {roles:['Morpheus']}]->(TheMatrixRevolutions),
				(KevinB)-[:ACTED_IN {roles:['Unknown']}]->(TheMatrixRevolutions),
				(Hugo)-[:ACTED_IN {roles:['Agent Smith']}]->(TheMatrixRevolutions),
				(LillyW)-[:DIRECTED]->(TheMatrixRevolutions),
				(LanaW)-[:DIRECTED]->(TheMatrixRevolutions),
				(JoelS)-[:PRODUCED]->(TheMatrixRevolutions)				    
				"""
			)
			.build();
	}

	@AfterAll
	static void stopNeo4j() {

		embeddedDatabaseServer.close();
	}
}
