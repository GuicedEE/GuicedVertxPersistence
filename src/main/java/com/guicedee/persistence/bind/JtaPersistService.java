/*
 * Copyright (C) 2010 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.guicedee.persistence.bind;

import com.google.inject.Provider;
import com.guicedee.persistence.PersistService;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.hibernate.reactive.mutiny.Mutiny;

import java.time.Duration;
import java.util.Map;

/**
 * Manages lifecycle for a JPA {@link EntityManagerFactory} backed by a persistence unit.
 */
@Log4j2
public class JtaPersistService implements PersistService {
    private final String persistenceUnitName;
    @Getter
    private final Map<?, ?> persistenceProperties;

    /**
     * Creates a persistence service for the given unit and properties.
     *
     * @param persistenceUnitName   the JPA persistence unit name
     * @param persistenceProperties properties used to create the factory
     */
    public JtaPersistService(
            String persistenceUnitName,
            Map<?, ?> persistenceProperties) {
        log.debug("📋 Creating JtaPersistService for persistence unit: '{}'", persistenceUnitName);
        this.persistenceUnitName = persistenceUnitName;
        this.persistenceProperties = persistenceProperties;
        sessionFactoryProvider = new SessionFactoryProvider(this);
    }

    private volatile EntityManagerFactory emFactory;
    private volatile Mutiny.SessionFactory sessionFactory;

    @Getter
    private final Provider<Mutiny.SessionFactory> sessionFactoryProvider;

    /**
     * Creates the EntityManagerFactory if it has not already been initialized.
     */
    @Override
    public synchronized Uni<Void> start() {
        log.trace("🚀 Starting JtaPersistService for persistence unit: '{}'", persistenceUnitName);
        if (null != emFactory) {
            log.debug("📋 EntityManagerFactory already exists for persistence unit: '{}', skipping initialization", persistenceUnitName);
            return Uni.createFrom().voidItem();
        }

        if (null != persistenceProperties) {
            return Uni.createFrom().item(() -> {
                log.debug("📋 Creating EntityManagerFactory for persistence unit: '{}'", persistenceUnitName);
                this.emFactory =
                        Persistence.createEntityManagerFactory(persistenceUnitName, persistenceProperties);
                this.sessionFactory = this.emFactory.unwrap(Mutiny.SessionFactory.class);
                log.info("✅ Successfully created EntityManagerFactory for persistence unit: '{}'", persistenceUnitName);
                return null;
            });
        } else {
            log.fatal("⚠️ No persistence properties provided for persistence unit: '{}'", persistenceUnitName);
            return Uni.createFrom().failure(new RuntimeException("No persistence properties provided for persistence unit: " + persistenceUnitName));
        }
    }

    /**
     * Closes the EntityManagerFactory if it is open.
     */
    @Override
    public  synchronized Uni<Void> stop() {
        log.info("🛑 Stopping JtaPersistService for persistence unit: '{}'", persistenceUnitName);
        if (null != emFactory && emFactory.isOpen()) {
            return Uni.createFrom().item(() -> {
                log.trace("📋 Closing EntityManagerFactory for persistence unit: '{}'", persistenceUnitName);
                sessionFactory.close();
                emFactory.close();
                log.info("✅ Successfully closed EntityManagerFactory for persistence unit: '{}'", persistenceUnitName);
                return null;
            });
        } else {
            log.warn("📋 No open EntityManagerFactory to close for persistence unit: '{}'", persistenceUnitName);
            return Uni.createFrom().voidItem();
        }
    }

    /**
     * Guice provider that exposes the EntityManagerFactory for the persistence unit.
     */
    public static class EntityManagerFactoryProvider implements Provider<EntityManagerFactory> {
        private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(EntityManagerFactoryProvider.class);
        private final JtaPersistService emProvider;

        /**
         * Creates the provider backed by the given persistence service.
         *
         * @param emProvider the persistence service providing the factory
         */
        public EntityManagerFactoryProvider(JtaPersistService emProvider) {
            this.emProvider = emProvider;
            log.debug("📋 Created EntityManagerFactoryProvider for persistence unit: '{}'", emProvider.persistenceUnitName);
        }

        /**
         * Returns the EntityManagerFactory instance.
         *
         * @return the EntityManagerFactory
         */
        @Override
        public EntityManagerFactory get() {
            log.trace("📋 Provider requested EntityManagerFactory for persistence unit: '{}'", emProvider.persistenceUnitName);
            assert null != emProvider.emFactory : "EntityManagerFactory is null for persistence unit: " + emProvider.persistenceUnitName;
            log.trace("📤 Providing EntityManagerFactory for persistence unit: '{}'", emProvider.persistenceUnitName);
            return emProvider.emFactory;
        }
    }
    /**
     * Guice provider that exposes the EntityManagerFactory for the persistence unit.
     */
    public static class SessionFactoryProvider implements Provider<Mutiny.SessionFactory> {
        private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(SessionFactoryProvider.class);
        private final JtaPersistService emProvider;

        /**
         * Creates the provider backed by the given persistence service.
         *
         * @param emProvider the persistence service providing the factory
         */
        public SessionFactoryProvider(JtaPersistService emProvider) {
            this.emProvider = emProvider;
            log.debug("📋 Created SessionFactoryProvider for persistence unit: '{}'", emProvider.persistenceUnitName);
        }

        /**
         * Returns the EntityManagerFactory instance.
         *
         * @return the EntityManagerFactory
         */
        @Override
        public Mutiny.SessionFactory get() {
            log.trace("📋 Provider requested SessionFactoryProvider for persistence unit: '{}'", emProvider.persistenceUnitName);
            if (emProvider.sessionFactory == null) {
                emProvider.start().await().atMost(Duration.ofMinutes(2));
            }
            assert null != emProvider.sessionFactory : "SessionFactoryProvider is null for persistence unit: " + emProvider.persistenceUnitName;
            log.trace("📤 Providing SessionFactoryProvider for persistence unit: '{}'", emProvider.persistenceUnitName);
            return emProvider.sessionFactory;
        }
    }
}
