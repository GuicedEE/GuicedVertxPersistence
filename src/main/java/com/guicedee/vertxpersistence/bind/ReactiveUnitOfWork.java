package com.guicedee.vertxpersistence.bind;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.persist.UnitOfWork;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.reactive.mutiny.Mutiny;

/**
 * Responsible for managing the entity managers active per db module for a unit of work that must commit to the database across all active transactions.
 * <p>
 * This unit of works contains the db connection (identified by annotation EntityManager) into the Session Factory.
 * <p>
 * When the connection is reactive, this will use Hibernate Reactive's Mutiny.Session instead of a standard EntityManager.
 * The Mutiny.Session is obtained from the Mutiny.SessionFactory which is the hibernate vertx reactive implementation.
 */
//@CallScope
@Slf4j
public class ReactiveUnitOfWork implements UnitOfWork
{
    private final String persistenceUnitName;

    private JtaPersistService persistService;

    public ReactiveUnitOfWork(String persistenceUnitName)
    {
        this.persistenceUnitName = persistenceUnitName;
    }

    /**
     * Sets the JtaPersistService for this UnitOfWork
     *
     * @param persistService The JtaPersistService to use
     */
    public void setPersistService(JtaPersistService persistService)
    {
        this.persistService = persistService;
    }

    @Override
    public void begin()
    {
        if (persistService != null)
        {
            persistService.start();
        }
    }


    @Singleton
    public static class EntityManagerFactoryProvider implements Provider<Mutiny.SessionFactory>
    {
        private final ReactiveUnitOfWork emProvider;

        public EntityManagerFactoryProvider(ReactiveUnitOfWork emProvider)
        {
            this.emProvider = emProvider;
        }

        @Override
        public Mutiny.SessionFactory get()
        {
            assert null != emProvider;
            return emProvider.persistService.getEmFactory().unwrap(Mutiny.SessionFactory.class);
        }
    }

}
