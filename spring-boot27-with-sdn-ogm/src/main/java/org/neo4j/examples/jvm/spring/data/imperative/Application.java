package org.neo4j.examples.jvm.spring.data.imperative;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.data.RepositoryMetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// SDN Neumann does not provide the necessary infrastructure for the repository metrics to work, so this must be disabled.
@SpringBootApplication(exclude = RepositoryMetricsAutoConfiguration.class)
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
