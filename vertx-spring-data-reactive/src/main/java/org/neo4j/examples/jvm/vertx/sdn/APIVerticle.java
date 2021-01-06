package org.neo4j.examples.jvm.vertx.sdn;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.HealthCheckHandler;
import io.vertx.ext.healthchecks.HealthChecks;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import org.neo4j.driver.Driver;
import org.neo4j.driver.reactive.RxResult;
import org.neo4j.driver.reactive.RxSession;
import org.neo4j.examples.jvm.vertx.sdn.movies.MovieRepository;
import org.neo4j.examples.jvm.vertx.sdn.movies.PeopleRepository;
import org.neo4j.examples.jvm.vertx.sdn.movies.Person;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public final class APIVerticle extends AbstractVerticle {

	private final int port;
	private final Scheduler scheduler;

	private final Driver driver;
	private final MovieRepository movieService;
	private final PeopleRepository peopleRepository;

	public APIVerticle(@Value("${server.port:8080}") int port,
		Driver driver, MovieRepository movieService,
		PeopleRepository peopleRepository
	) {
		this.port = port;
		this.driver = driver;
		this.scheduler = Schedulers.fromExecutor(command -> this.context.runOnContext(v -> command.run()));

		this.movieService = movieService;
		this.peopleRepository = peopleRepository;
	}

	@Override
	public void start(Promise<Void> startPromise) {

		var liveness = HealthChecks.create(vertx);
		liveness.register("alive", promise -> promise.complete(Status.OK()));

		var readiness = HealthChecks.create(vertx);
		readiness.register("alive", this::runHealthCheckQuery);

		var router = Router.router(vertx);

		router.get("/api/movies").handler(this::getAllMovies);
		router.post("/api/people").handler(BodyHandler.create()).handler(this::createNewPerson);
		router.get("/management/health/liveness").handler(HealthCheckHandler.createWithHealthChecks(liveness));
		router.get("/management/health/readiness").handler(HealthCheckHandler.createWithHealthChecks(readiness));

		vertx
			.createHttpServer()
			.requestHandler(router)
			.listen(port, http -> {
				if (http.succeeded()) {
					startPromise.complete();
				} else {
					startPromise.fail(http.cause());
				}
			});
	}

	void getAllMovies(RoutingContext ctx) {

		var response = ctx.response().putHeader("content-type", "application/json")
			.setChunked(true)
			.setStatusCode(200);

		// You could also do
		// movieService.findAll().collectList()
		// and serialize the Mono<List>, but chunking it that way makes better use
		// of the reactive capability
		Mono.just("[").concatWith(
			this.movieService.findAll(Sort.by("title").ascending())
				.map(Json::encodePrettily)
				.switchOnFirst((signal, original) -> Flux.<String>just(signal.get())
					.concatWith(original.skip(1).map(value -> "," + value))))
			.concatWith(Mono.just("]"))
			.publishOn(this.scheduler)
			.doOnComplete(response::end)
			.subscribe(response::write);
	}

	void createNewPerson(RoutingContext ctx) {

		Mono.fromSupplier(() -> Json.decodeValue(ctx.getBody(), Person.class))
			.flatMap(this.peopleRepository::save)
			.publishOn(this.scheduler)
			.subscribe(p -> ctx.response().putHeader("content-type", "application/json")
				.setStatusCode(201)
				.end(Json.encodePrettily(p)));
	}

	void runHealthCheckQuery(Promise<Status> statusPromise) {

		Flux.usingWhen(Mono.fromSupplier(driver::rxSession),
			s -> s.writeTransaction(tx -> {
				RxResult result = tx
					.run("CALL dbms.components() YIELD name, edition WHERE name = 'Neo4j Kernel' RETURN edition");
				return Mono.from(result.records()).map((record) -> record.get("edition").asString())
					.zipWhen((edition) -> Mono.from(result.consume()));
			}), RxSession::close)
			.single()
			.publishOn(this.scheduler)
			.doOnError(e -> statusPromise.complete(Status.KO()))
			.subscribe(t -> {
				var edition = t.getT1();
				var serverInfo = t.getT2().server();

				statusPromise.complete(Status.OK(new JsonObject()
						.put("server", serverInfo.version() + "@" + serverInfo.address())
						.put("edition", edition)
					)
				);
			});
	}
}
