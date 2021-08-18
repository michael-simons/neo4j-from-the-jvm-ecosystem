package org.neo4j.examples.jvm.tck.movies;

import java.util.List;

/**
 * @author Michael J. Simons
 */
public record PersonDetails(

	String name,

	Integer born,

	List<Movie> actedIn,

	List<Movie> directed,

	List<Person> related
) {
}
