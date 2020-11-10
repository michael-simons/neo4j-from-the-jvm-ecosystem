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
package org.neo4j.examples.jvm.quarkus.reactive.movies;

import io.smallrye.mutiny.Uni;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Michael J. Simons
 */
@RequestScoped
@Path("/api/people")
public class PeopleResource {

	private final PeopleRepository peopleRepository;

	public PeopleResource(PeopleRepository peopleRepository) {
		this.peopleRepository = peopleRepository;
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Uni<Response> createNewPerson(Person newPerson) {

		var savedPerson = peopleRepository.save(newPerson);
		return savedPerson.map(entity -> Response.status(Response.Status.CREATED).entity(entity).build());
	}
}
