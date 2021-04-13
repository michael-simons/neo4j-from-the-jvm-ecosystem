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
import java.util.Collections;
import java.util.List;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import org.springframework.data.neo4j.core.schema.Relationship.Direction;

/**
 * @author Michael J. Simons
 */
@Node
public final record Movie(

	@Id
	String title,

	@Property("tagline")
	String description,

	@Relationship(value = "ACTED_IN", direction = Direction.INCOMING)
	List<Actor> actors,

	@Relationship(value = "DIRECTED", direction = Direction.INCOMING)
	List<Person> directors,

	Integer released) {

	public Movie(String title, String description) {
		this(title, description, List.of(), List.of(), null);
	}

	@PersistenceConstructor
	public Movie(String title, String description, List<Actor> actors, List<Person> directors, Integer released) {
		this.title = title;
		this.description = description;
		this.actors = actors == null ? List.of() : new ArrayList<>(actors);
		this.directors = directors == null ? List.of() : new ArrayList<>(directors);
		this.released = released;
	}
}
