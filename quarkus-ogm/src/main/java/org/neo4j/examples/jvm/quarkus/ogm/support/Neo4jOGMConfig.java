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
package org.neo4j.examples.jvm.quarkus.ogm.support;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Disposes;
import javax.enterprise.inject.Produces;

import org.neo4j.driver.Driver;
import org.neo4j.ogm.drivers.bolt.driver.BoltDriver;
import org.neo4j.ogm.session.SessionFactory;

/**
 * @author Michael J. Simons
 */
public class Neo4jOGMConfig {

	@Produces
	@ApplicationScoped
	SessionFactory produceSessionFactory(Driver driver) {
		return new SessionFactory(new BoltDriver(driver), "org.neo4j.examples.jvm.quarkus.ogm.movies");
	}

	/*
	// The session represents the transaction boundary of OGM. You could use it directly in a resource, effectively so doing Open Session In View.
	@Produces
	@RequestScoped
	Session openSession(SessionFactory sessionFactory) {
		return sessionFactory.openSession();
	}
	*/

	void disposeSessionFactory(@Disposes SessionFactory sessionFactory) {
		sessionFactory.close();
	}
}
