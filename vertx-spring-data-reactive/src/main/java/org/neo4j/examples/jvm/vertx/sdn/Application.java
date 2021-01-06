package org.neo4j.examples.jvm.vertx.sdn;

import io.vertx.core.logging.SLF4JLogDelegateFactory;

import org.neo4j.driver.Driver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.neo4j.core.ReactiveDatabaseSelectionProvider;
import org.springframework.data.neo4j.core.transaction.ReactiveNeo4jTransactionManager;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {

		String logFactory = System.getProperty("org.vertx.logger-delegate-factory-class-name");
		if (logFactory == null) {
			System.setProperty("org.vertx.logger-delegate-factory-class-name", SLF4JLogDelegateFactory.class.getName());
		}

		SpringApplication.run(Application.class, args);
	}

	@Bean
	ReactiveNeo4jTransactionManager reactiveTransactionManager(
		Driver driver, ReactiveDatabaseSelectionProvider databaseSelectionProvider) {
		return new ReactiveNeo4jTransactionManager(driver, databaseSelectionProvider);
	}
}

