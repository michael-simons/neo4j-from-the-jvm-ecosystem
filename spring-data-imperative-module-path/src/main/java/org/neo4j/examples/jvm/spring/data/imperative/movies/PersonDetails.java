package org.neo4j.examples.jvm.spring.data.imperative.movies;

import java.util.List;

/**
 * This is a DTO based projection, containing a couple of additional details,
 * like the list of movies a person acted in, the movies they direct and which other
 * people they acted with
 *
 * @author Michael J. Simons
 */
public record PersonDetails(

	String name,

	Integer born,

	List<Movie> actedIn,

	List<Movie> directed,

	List<Person> related) {
}
