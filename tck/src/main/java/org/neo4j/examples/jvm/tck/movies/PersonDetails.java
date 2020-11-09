package org.neo4j.examples.jvm.tck.movies;

import lombok.Builder;
import lombok.Data;

import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * @author Michael J. Simons
 */
@Data
@Builder(builderClassName = "Builder", setterPrefix = "with")
@JsonDeserialize(builder = PersonDetails.Builder.class)
public final class PersonDetails {

	private final String name;

	private final Integer born;

	private final List<Movie> actedIn;

	private final List<Movie> directed;

	private final List<Person> related;
}
