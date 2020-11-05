/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.examples.jvm.quarkus.imperative.movies;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.enterprise.context.ApplicationScoped;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Value;
import org.neo4j.driver.reactive.RxSession;
import org.reactivestreams.FlowAdapters;

/**
 * @author Michael J. Simons
 */
@ApplicationScoped
public class MovieRepository {

	private final Driver driver;

	MovieRepository(Driver driver) {
		this.driver = driver;
	}

	public Multi<Movie> findAll() {

		var sessionHolder = new AtomicReference<RxSession>();
		return Uni.createFrom()
			.deferred(() -> {
				var session = driver.rxSession();
				sessionHolder.set(session);
				return Uni.createFrom().item(session);
			})
			.toMulti()
			.flatMap(this::executeQuery)
			.onTermination().invoke(() -> sessionHolder.get().close());
	}

	private Multi<Movie> executeQuery(RxSession rxSession) {
		var query = ""
					+ "match (m:Movie) "
					+ "match (m) <- [:DIRECTED] - (d:Person) "
					+ "match (m) <- [r:ACTED_IN] - (a:Person) "
					+ "return m, collect(d) as directors, collect({name:a.name, roles: r.roles}) as actors";

		return Multi
			.createFrom().publisher(rxSession.readTransaction(tx -> tx.run(query).records()))
			.map(r -> {
				var movieNode = r.get("m").asNode();

				var directors = r.get("directors").asList(v -> {
					var personNode = v.asNode();
					return new Person(personNode.get("born").asInt(), personNode.get("name").asString());
				});

				var actors = r.get("actors").asList(v -> new Actor(v.get("name").asString(), v.get("roles").asList(Value::asString)));

				var m = new Movie(movieNode.get("title").asString(), movieNode.get("tagline").asString());
				m.setReleased(movieNode.get("released").asInt());
				m.addDirectors(directors);
				m.addActors(actors);
				return m;
			});
	}
}
