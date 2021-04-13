module sdn.mp {

	requires com.fasterxml.jackson.annotation;
	requires com.fasterxml.jackson.databind;

	// The Cypher-DSL provides a valid Java module.
	// SDN 6.1 is not build as Multi-Release jar and thus does
	// not contain a module-info which could require it, therefore we
	// must do this manually.
	requires org.neo4j.cypherdsl.core;

	requires spring.beans;
	requires spring.boot;
	requires spring.boot.autoconfigure;
	requires spring.data.commons;
	requires spring.data.neo4j;
	requires spring.web;
	requires spring.webmvc;

	// Otherwise this is not visible to Spring Boot
	requires org.apache.tomcat.embed.core;

	// I want to have reflection only against selected modules
	opens org.neo4j.examples.jvm.spring.data.imperative to spring.core;
	opens org.neo4j.examples.jvm.spring.data.imperative.movies to spring.core, com.fasterxml.jackson.databind;

	// We can be more selective here as well, but the application in the root package and the movies
	// will be public api.
	exports org.neo4j.examples.jvm.spring.data.imperative;
	exports org.neo4j.examples.jvm.spring.data.imperative.movies;
}