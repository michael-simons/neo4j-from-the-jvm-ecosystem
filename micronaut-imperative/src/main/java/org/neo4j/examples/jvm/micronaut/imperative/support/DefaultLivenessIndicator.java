package org.neo4j.examples.jvm.micronaut.imperative.support;

import io.micronaut.health.HealthStatus;
import io.micronaut.management.health.indicator.HealthIndicator;
import io.micronaut.management.health.indicator.HealthResult;
import io.micronaut.management.health.indicator.annotation.Liveness;
import io.reactivex.Single;

import org.reactivestreams.Publisher;

/**
 * Seems that Micronaut needs at last one component annotated with {@link Liveness @Liveness}, otherwise live status is unknown.
 *
 * @author Michael J. Simons
 */
@Liveness
public final class DefaultLivenessIndicator implements HealthIndicator {
	@Override
	public Publisher<HealthResult> getResult() {
		return Single.just(HealthResult.builder("default").status(HealthStatus.UP).build()).toFlowable();
	}
}
