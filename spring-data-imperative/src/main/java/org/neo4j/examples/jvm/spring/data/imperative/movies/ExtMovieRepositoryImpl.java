package org.neo4j.examples.jvm.spring.data.imperative.movies;

import java.util.List;

import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;

final class ExtMovieRepositoryImpl implements ExtMovieRepository {

	private final Neo4jClient client;

	private final Neo4jTemplate template;

	ExtMovieRepositoryImpl(Neo4jClient client, Neo4jTemplate template) {
		this.client = client;
		this.template = template;
	}

	@Override
	public MovieDescription updateDescription(String title, String description) {

		var currentVersion = client.query("MATCH (n:Movie WHERE n.title = $title) RETURN n.version").bind(title).to("title")
			.fetchAs(Long.class).one().orElse(null);

		var instance = new Movie(currentVersion, title, description, List.of(), List.of());
		return this.template.saveAs(instance, MovieDescription.class);
	}
}
