package org.neo4j.examples.jvm.spring.data.imperative.support;

import java.util.List;

import org.neo4j.driver.Driver;
import org.neo4j.ogm.config.AutoIndexMode;
import org.neo4j.ogm.drivers.bolt.driver.BoltDriver;
import org.neo4j.ogm.session.SessionFactory;
import org.neo4j.ogm.session.event.EventListener;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScanPackages;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.transaction.Neo4jTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

/**
 * This provides the beans necessary to run SDN+OGM much like Spring Boot prior to 2.4 did.
 *
 * @author Michael J. Simons
 */
@Configuration(proxyBeanMethods = false)
public class Neo4jOGMConfig {

	@Bean
	@ConditionalOnMissingBean(PlatformTransactionManager.class)
	public Neo4jTransactionManager transactionManager(SessionFactory sessionFactory,
		ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
		Neo4jTransactionManager transactionManager = new Neo4jTransactionManager(sessionFactory);
		transactionManagerCustomizers.ifAvailable((customizers) -> customizers.customize(transactionManager));
		return transactionManager;
	}

	@Bean
	@ConditionalOnMissingBean
	public org.neo4j.ogm.config.Configuration configuration() {
		return new org.neo4j.ogm.config.Configuration.Builder()
			.useNativeTypes()
			.build();
	}

	@Bean
	@ConditionalOnMissingBean
	public BoltDriver ogmDriver(org.neo4j.ogm.config.Configuration ogmConfiguration, Driver nativeDriver) {

		BoltDriver boltDriver = new BoltDriver(nativeDriver) {
			@Override
			public synchronized void close() {
				// We must prevent the bolt driver from closing the driver bean
			}
		};
		boltDriver.configure(ogmConfiguration);
		return boltDriver;
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnMissingBean(SessionFactory.class)
	static class Neo4jOgmSessionFactoryConfiguration {

		@Bean
		SessionFactory sessionFactory(org.neo4j.ogm.config.Configuration configuration, BoltDriver ogmDriver,
			BeanFactory beanFactory,
			ObjectProvider<EventListener> eventListeners) {
			SessionFactory sessionFactory = new SessionFactory(ogmDriver, getPackagesToScan(beanFactory));
			if (configuration.getAutoIndex() != AutoIndexMode.NONE) {
				sessionFactory.runAutoIndexManager(configuration);
			}
			eventListeners.orderedStream().forEach(sessionFactory::register);
			return sessionFactory;
		}

		private String[] getPackagesToScan(BeanFactory beanFactory) {
			List<String> packages = EntityScanPackages.get(beanFactory).getPackageNames();
			if (packages.isEmpty() && AutoConfigurationPackages.has(beanFactory)) {
				packages = AutoConfigurationPackages.get(beanFactory);
			}
			return StringUtils.toStringArray(packages);
		}
	}
}
