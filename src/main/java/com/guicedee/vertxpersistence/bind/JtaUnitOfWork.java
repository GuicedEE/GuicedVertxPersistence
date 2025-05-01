package com.guicedee.vertxpersistence.bind;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import com.google.inject.persist.UnitOfWork;
import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedservlets.servlets.services.scopes.CallScope;
import com.guicedee.guicedservlets.websockets.options.CallScopeProperties;
import com.guicedee.vertxpersistence.annotations.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.session.impl.ReactiveSessionFactoryImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.guicedee.vertxpersistence.bind.JtaPersistService.ENTITY_MANAGER_KEY;

/**
 * Responsible for managing the entity managers active per db module for a unit of work that must commit to the database across all active transactions.
 * 
 * This unit of works contains the db connection (identified by annotation EntityManager) into the Session Factory.
 * 
 * When the connection is reactive, this will use Hibernate Reactive's Mutiny.Session instead of a standard EntityManager.
 * The Mutiny.Session is obtained from the Mutiny.SessionFactory which is the hibernate vertx reactive implementation.
 */
@CallScope
public class JtaUnitOfWork implements UnitOfWork
{
    private static final Map<EntityManager, Mutiny.SessionFactory> entityManagerSessionFactoryMap = new HashMap<>();

    private final String persistenceUnitName;
    private final List<EntityManager> activeTransactions = new ArrayList<>();
    private final boolean reactive;

    private volatile EntityManagerFactory emFactory;
    @Getter
    private volatile Mutiny.SessionFactory sessionFactory;
    private JtaPersistService persistService;

    public JtaUnitOfWork(String persistenceUnitName, boolean reactive)
    {
        this.persistenceUnitName = persistenceUnitName;
        this.reactive = reactive;
    }

    /**
     * Sets the EntityManagerFactory for this UnitOfWork
     * 
     * @param emFactory The EntityManagerFactory to use
     */
    public void setEntityManagerFactory(org.hibernate.reactive.mutiny.impl.MutinySessionFactoryImpl emFactory) {
        if (reactive && (emFactory instanceof Mutiny.SessionFactory)) {
            this.sessionFactory = (Mutiny.SessionFactory) emFactory;
        }
    }   /**
     * Sets the EntityManagerFactory for this UnitOfWork
     *
     * @param emFactory The EntityManagerFactory to use
     */
    public void setEntityManagerFactory(EntityManagerFactory emFactory) {
        this.emFactory = emFactory;
        if (reactive && (emFactory instanceof Mutiny.SessionFactory)) {
            this.sessionFactory = (Mutiny.SessionFactory) emFactory;
        }
    }

    /**
     * Sets the JtaPersistService for this UnitOfWork
     * 
     * @param persistService The JtaPersistService to use
     */
    public void setPersistService(JtaPersistService persistService) {
        this.persistService = persistService;
    }

    @Override
    public void begin()
    {
        CallScopeProperties csp = IGuiceContext.get(CallScopeProperties.class);
        Preconditions.checkState(
                null == csp.getProperties().get(ENTITY_MANAGER_KEY),
                "Work already begun on this thread. Looks like you have called UnitOfWork.begin() twice"
                        + " without a balancing call to end() in between.");

        // If emFactory is null, try to get it from persistService
        if (emFactory == null && persistService != null) {
            // Access the emFactory field from JtaPersistService
            try {
                java.lang.reflect.Field field = JtaPersistService.class.getDeclaredField("emFactory");
                field.setAccessible(true);
                EntityManagerFactory factory = (EntityManagerFactory) field.get(persistService);
                if (factory != null) {
                    setEntityManagerFactory(factory);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to get EntityManagerFactory from JtaPersistService", e);
            }
        }

        if (reactive && sessionFactory != null) {
            // For reactive connections, use Mutiny.Session
            var session = sessionFactory.openSession();
            csp.getProperties().put(ENTITY_MANAGER_KEY, session);
            session.await().indefinitely();
        } else if (emFactory != null) {
            // For non-reactive connections, use standard EntityManager
            var em = emFactory.createEntityManager();
            csp.getProperties().put(ENTITY_MANAGER_KEY, em);
        } else {
            throw new IllegalStateException("EntityManagerFactory is not set. Make sure PersistService.start() is called before UnitOfWork.begin().");
        }
    }

    @Override
    public void end()
    {
        CallScopeProperties csp = IGuiceContext.get(CallScopeProperties.class);
        Object em = csp.getProperties().get(ENTITY_MANAGER_KEY);
        // Let's not penalize users for calling end() multiple times.
        if (null == em)
        {
            return;
        }

        try
        {
            if (reactive && em instanceof Mutiny.Session) {
                ((Mutiny.Session) em).close();
            } else if (em instanceof jakarta.persistence.EntityManager) {
                ((jakarta.persistence.EntityManager) em).close();
            }
        }
        finally
        {
            csp.getProperties().remove(ENTITY_MANAGER_KEY);
        }
    }

    /**
     * Checks if this UnitOfWork is for a reactive connection
     * 
     * @return true if this UnitOfWork is for a reactive connection
     */
    public boolean isReactive() {
        return reactive;
    }
}
