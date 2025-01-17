package org.neo4j.examples.jvm.spring.data.imperative.movies;

interface ExtMovieRepository {

	MovieDescription updateDescription(String title, String description);
}
