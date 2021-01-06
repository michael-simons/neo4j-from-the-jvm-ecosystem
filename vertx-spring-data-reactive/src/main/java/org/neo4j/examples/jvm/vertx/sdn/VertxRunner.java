package org.neo4j.examples.jvm.vertx.sdn;

import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * This kicks off VertX.
 */
@Component
public final class VertxRunner implements CommandLineRunner {

	private final ObjectProvider<Verticle> allVerticles;

	public VertxRunner(ObjectProvider<Verticle> allVerticles) {
		this.allVerticles = allVerticles;
	}

	@Override
	public void run(String... args) {
		final Vertx vertx = Vertx.vertx();
		allVerticles.forEach(vertx::deployVerticle);
	}
}
