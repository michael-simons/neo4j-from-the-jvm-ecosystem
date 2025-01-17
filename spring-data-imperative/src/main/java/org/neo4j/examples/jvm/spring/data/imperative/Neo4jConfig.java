package org.neo4j.examples.jvm.spring.data.imperative;

import org.neo4j.cypherdsl.core.renderer.Configuration;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration(proxyBeanMethods = false)
public class Neo4jConfig {

	@Bean
	Configuration cypherDslConfiguration() {
		return Configuration.newConfig().withDialect(Dialect.NEO4J_5).build();
	}
}
