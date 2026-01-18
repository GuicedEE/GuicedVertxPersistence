package com.guicedee.vertxpersistence.bind;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.hibernate.reactive.mutiny.Mutiny;

/**
 * Provider for Mutiny.SessionFactory that gets the factory from JtaPersistService.
 */
@Singleton
public class MutinySessionFactoryProvider implements Provider<Mutiny.SessionFactory> {
    private final JtaPersistService persistService;

    /**
     * Creates the provider using the persistence service.
     *
     * @param persistService the persistence service supplying the entity manager factory
     */
    public MutinySessionFactoryProvider(JtaPersistService persistService) {
        this.persistService = persistService;
    }

    /**
     * Returns the Mutiny SessionFactory unwrapped from the JPA EntityManagerFactory.
     *
     * @return the reactive SessionFactory
     */
    @Override
    public Mutiny.SessionFactory get() {
        return persistService.getEmFactory().unwrap(Mutiny.SessionFactory.class);
    }
}
