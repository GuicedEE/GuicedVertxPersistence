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

    private java.util.concurrent.CompletableFuture<Void> starting;
    private java.util.concurrent.CompletableFuture<Void> stopping;
    private boolean stopRequested;

    /** One subscribed factory creation, with stop/start races resolved before readiness. */
    @Override public Uni<Void> start() {
        return Uni.createFrom().completionStage(this::startOnce);
    }
    private synchronized java.util.concurrent.CompletableFuture<Void> startOnce() {
        if (stopRequested) return java.util.concurrent.CompletableFuture.failedFuture(new IllegalStateException("Persistence is stopped"));
        if (starting != null) return starting;
        starting = new java.util.concurrent.CompletableFuture<>();
        Runnable initialize = () -> {
            try {
                EntityManagerFactory created = createFactory();
                synchronized (this) { emFactory = created; }
                Mutiny.SessionFactory reactive = created.unwrap(Mutiny.SessionFactory.class);
                synchronized (this) {
                    sessionFactory = reactive;
                    if (stopRequested) starting.completeExceptionally(new IllegalStateException("Persistence stopped during startup"));
                    else starting.complete(null);
                }
            } catch (Throwable failed) {
                try { closeFactory(); } catch (Throwable cleanup) { failed.addSuppressed(cleanup); }
                starting.completeExceptionally(failed);
            }
        };
        var context = io.vertx.core.Vertx.currentContext();
        if (context != null) {
            try {
                context.executeBlocking(() -> { initialize.run();return null; }, false)
                        .onFailure(starting::completeExceptionally);
            } catch (Throwable failed) { starting.completeExceptionally(failed); }
        } else Thread.ofVirtual().name("guicedee-persistence-start").start(initialize);
        return starting;
    }
    /** Isolated override point for lifecycle verification; normal startup uses the JPA provider. */
    protected EntityManagerFactory createFactory() {
        if (persistenceProperties == null) throw new IllegalStateException("Persistence properties are required");
        return Persistence.createEntityManagerFactory(persistenceUnitName, persistenceProperties);
    }
    /** Terminal, idempotent stop. Subscription waits for in-flight creation and closes the factory once. */
    @Override public synchronized Uni<Void> stop() {
        stopRequested = true;
        return Uni.createFrom().completionStage(this::stopOnce);
    }
    private synchronized java.util.concurrent.CompletableFuture<Void> stopOnce() {
        if (stopping != null) return stopping;
        stopping = new java.util.concurrent.CompletableFuture<>();
        var startup = starting == null ? java.util.concurrent.CompletableFuture.<Void>completedFuture(null) : starting;
        startup.whenComplete((ignored, failedStartup) -> Thread.ofVirtual().name("guicedee-persistence-stop").start(() -> {
            try { closeFactory();stopping.complete(null); }
            catch (Throwable failed) { stopping.completeExceptionally(failed); }
        }));
        return stopping;
    }
    private void closeFactory() {
        EntityManagerFactory owned;
        synchronized (this) { owned = emFactory;emFactory = null;sessionFactory = null; }
        // The Mutiny wrapper and EMF refer to the same underlying factory. Close its owner once.
        if (owned != null && owned.isOpen()) owned.close();
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
