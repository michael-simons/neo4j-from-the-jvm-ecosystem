package org.neo4j.examples.jvm.spring.data.imperative;

import org.neo4j.driver.Driver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.core.DatabaseSelectionProvider;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	ReactiveNeo4jTransactionManager reactiveTransactionManager(Driver driver, ReactiveDatabaseSelectionProvider databaseSelectionProvider) {
		return new ReactiveNeo4jTransactionManager(driver, databaseSelectionProvider);
	}
}
