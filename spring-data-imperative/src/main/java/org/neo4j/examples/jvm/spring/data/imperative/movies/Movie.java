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
package org.neo4j.examples.jvm.spring.data.imperative.movies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.data.annotation.PersistenceCreator;
import org.springframework.data.annotation.Version;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;

/**
 * @author Michael J. Simons
 */
@Node
public final class Movie {

	@Version
	private final Long version;

	@Id
	private final String title;

	@Property("tagline")
	private final String description;

	@Relationship(value = "ACTED_IN", direction = Direction.INCOMING)
	private final List<Actor> actors;

	@Relationship(value = "DIRECTED", direction = Direction.INCOMING)
	private final List<Person> directors;

	private Integer released;

	public Movie(String title, String description) {
		this.version = null;
		this.title = title;
		this.description = description;
		this.actors = new ArrayList<>();
		this.directors = new ArrayList<>();
	}

	@PersistenceCreator
	Movie(Long version, String title, String description, List<Actor> actors, List<Person> directors) {
		this.version = version;
		this.title = title;
		this.description = description;
		this.actors = actors == null ? new ArrayList<>() : new ArrayList<>(actors);
		this.directors = directors == null ? new ArrayList<>() : new ArrayList<>(directors);
	}

	public Long getVersion() {
		return version;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public List<Actor> getActors() {
		return List.copyOf(this.actors);
	}

	public List<Person> getDirectors() {
		return List.copyOf(this.directors);
	}

	public Integer getReleased() {
		return released;
	}

	public void setReleased(Integer released) {
		this.released = released;
	}

	public Movie addActors(Collection<Actor> actors) {
		this.actors.addAll(actors);
		return this;
	}

	public Movie addDirectors(Collection<Person> directors) {
		this.directors.addAll(directors);
		return this;
	}
}
