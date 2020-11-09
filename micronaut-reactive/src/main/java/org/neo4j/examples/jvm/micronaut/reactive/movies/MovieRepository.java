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
package org.neo4j.examples.jvm.micronaut.reactive.movies;

import io.reactivex.Flowable;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Singleton;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Value;
import org.neo4j.driver.reactive.RxSession;

/**
 * @author Michael J. Simons
 */
@Singleton
public class MovieRepository {

	private final Driver driver;

	MovieRepository(Driver driver) {
		this.driver = driver;
	}

	public Flowable<Movie> findAll() {

		var sessionHolder = new AtomicReference<RxSession>();
		return Single
			.defer(() -> {
				var session = driver.rxSession();
				sessionHolder.set(session);
				return Single.just(session);
			})
			.flatMapPublisher(this::executeQuery)
			.doAfterTerminate(() -> Observable.fromPublisher(sessionHolder.get().close()).subscribe());
	}

	private Flowable<Movie> executeQuery(RxSession rxSession) {
		var query = ""
					+ "match (m:Movie) "
					+ "match (m) <- [:DIRECTED] - (d:Person) "
					+ "match (m) <- [r:ACTED_IN] - (a:Person) "
					+ "RETURN m, collect(DISTINCT d) AS directors, collect(DISTINCT {name:a.name, roles: r.roles}) AS actors\n"
					+ "ORDER BY m.name ASC";

		return Flowable
			.fromPublisher(rxSession.readTransaction(tx -> tx.run(query).records()))
			.map(r -> {
				var movieNode = r.get("m").asNode();

				var directors = r.get("directors").asList(v -> {
					var personNode = v.asNode();
					return new Person(personNode.get("born").asInt(), personNode.get("name").asString());
				});

				var actors = r.get("actors")
					.asList(v -> new Actor(v.get("name").asString(), v.get("roles").asList(Value::asString)));

				var m = new Movie(movieNode.get("title").asString(), movieNode.get("tagline").asString());
				m.setReleased(movieNode.get("released").asInt());
				m.addDirectors(directors);
				m.addActors(actors);
				return m;
			});
	}
}
