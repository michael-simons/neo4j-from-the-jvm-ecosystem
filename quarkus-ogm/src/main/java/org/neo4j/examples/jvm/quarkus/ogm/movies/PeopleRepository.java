package org.neo4j.examples.jvm.quarkus.ogm.movies;

import javax.enterprise.context.ApplicationScoped;

import org.neo4j.ogm.session.Session;
import org.neo4j.ogm.session.SessionFactory;

/**
 * @author Michael J. Simons
 */
@ApplicationScoped
public class PeopleRepository {

	private final SessionFactory sessionFactory;

	public PeopleRepository(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	Person save(Person person) {

		var session = sessionFactory.openSession();
		session.save(person);
		return person;
	}
}
