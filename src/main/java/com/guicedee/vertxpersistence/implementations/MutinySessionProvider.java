package com.guicedee.vertxpersistence.implementations;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.UnitOfWork;
import com.guicedee.client.CallScoper;
import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedservlets.servlets.services.scopes.CallScope;
import com.guicedee.client.CallScopeProperties;
import com.guicedee.vertxpersistence.bind.JtaPersistService;
import com.guicedee.vertxpersistence.bind.JtaUnitOfWork;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.CriteriaUpdate;
import jakarta.persistence.metamodel.Attribute;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.CacheMode;
import org.hibernate.Filter;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.reactive.common.AffectedEntities;
import org.hibernate.reactive.common.Identifier;
import org.hibernate.reactive.common.ResultSetMapping;
import org.hibernate.reactive.mutiny.Mutiny;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

@Slf4j
public class MutinySessionProvider implements Provider<Mutiny.Session>
{
    @Inject
    private UnitOfWork unitOfWork;

    @Override
    public Mutiny.Session get()
    {
        if (IGuiceContext.instance().isBuildingInjector())
        {
            return new Mutiny.Session()
            {
                @Override
                public <T> Uni<T> find(Class<T> entityClass, Object id)
                {
                    return null;
                }

                @Override
                public <T> Uni<T> find(Class<T> entityClass, Object id, LockMode lockMode)
                {
                    return null;
                }

                @Override
                public <T> Uni<T> find(EntityGraph<T> entityGraph, Object id)
                {
                    return null;
                }

                @Override
                public <T> Uni<List<T>> find(Class<T> entityClass, Object... ids)
                {
                    return null;
                }

                @Override
                public <T> Uni<T> find(Class<T> entityClass, Identifier<T> naturalId)
                {
                    return null;
                }

                @Override
                public <T> T getReference(Class<T> entityClass, Object id)
                {
                    return null;
                }

                @Override
                public <T> T getReference(T entity)
                {
                    return null;
                }

                @Override
                public Uni<Void> persist(Object entity)
                {
                    return null;
                }

                @Override
                public Uni<Void> persistAll(Object... entities)
                {
                    return null;
                }

                @Override
                public Uni<Void> remove(Object entity)
                {
                    return null;
                }

                @Override
                public Uni<Void> removeAll(Object... entities)
                {
                    return null;
                }

                @Override
                public <T> Uni<T> merge(T entity)
                {
                    return null;
                }

                @Override
                public Uni<Void> mergeAll(Object... entities)
                {
                    return null;
                }

                @Override
                public Uni<Void> refresh(Object entity)
                {
                    return null;
                }

                @Override
                public Uni<Void> refresh(Object entity, LockMode lockMode)
                {
                    return null;
                }

                @Override
                public Uni<Void> refreshAll(Object... entities)
                {
                    return null;
                }

                @Override
                public Uni<Void> lock(Object entity, LockMode lockMode)
                {
                    return null;
                }

                @Override
                public Uni<Void> flush()
                {
                    return null;
                }

                @Override
                public <T> Uni<T> fetch(T association)
                {
                    return null;
                }

                @Override
                public <E, T> Uni<T> fetch(E entity, Attribute<E, T> field)
                {
                    return null;
                }

                @Override
                public <T> Uni<T> unproxy(T association)
                {
                    return null;
                }

                @Override
                public LockMode getLockMode(Object entity)
                {
                    return null;
                }

                @Override
                public boolean contains(Object entity)
                {
                    return false;
                }

                @Override
                public <R> Mutiny.SelectionQuery<R> createSelectionQuery(String queryString, Class<R> resultType)
                {
                    return null;
                }

                @Override
                public Mutiny.MutationQuery createMutationQuery(String queryString)
                {
                    return null;
                }

                @Override
                public <R> Mutiny.Query<R> createQuery(String queryString)
                {
                    return null;
                }

                @Override
                public <R> Mutiny.SelectionQuery<R> createQuery(String queryString, Class<R> resultType)
                {
                    return null;
                }

                @Override
                public <R> Mutiny.SelectionQuery<R> createQuery(CriteriaQuery<R> criteriaQuery)
                {
                    return null;
                }

                @Override
                public <R> Mutiny.MutationQuery createQuery(CriteriaUpdate<R> criteriaUpdate)
                {
                    return null;
                }

                @Override
                public <R> Mutiny.MutationQuery createQuery(CriteriaDelete<R> criteriaDelete)
                {
                    return null;
                }

                @Override
                public <R> Mutiny.Query<R> createNamedQuery(String queryName)
                {
                    return null;
                }

                @Override
                public <R> Mutiny.SelectionQuery<R> createNamedQuery(String queryName, Class<R> resultType)
                {
                    return null;
                }

                @Override
                public <R> Mutiny.Query<R> createNativeQuery(String queryString)
                {
                    return null;
                }

                @Override
                public <R> Mutiny.Query<R> createNativeQuery(String queryString, AffectedEntities affectedEntities)
                {
                    return null;
                }

                @Override
                public <R> Mutiny.SelectionQuery<R> createNativeQuery(String queryString, Class<R> resultType)
                {
                    return null;
                }

                @Override
                public <R> Mutiny.SelectionQuery<R> createNativeQuery(String queryString, Class<R> resultType, AffectedEntities affectedEntities)
                {
                    return null;
                }

                @Override
                public <R> Mutiny.SelectionQuery<R> createNativeQuery(String queryString, ResultSetMapping<R> resultSetMapping)
                {
                    return null;
                }

                @Override
                public <R> Mutiny.SelectionQuery<R> createNativeQuery(String queryString, ResultSetMapping<R> resultSetMapping, AffectedEntities affectedEntities)
                {
                    return null;
                }

                @Override
                public Mutiny.Session setFlushMode(FlushMode flushMode)
                {
                    return null;
                }

                @Override
                public FlushMode getFlushMode()
                {
                    return null;
                }

                @Override
                public Mutiny.Session detach(Object entity)
                {
                    return null;
                }

                @Override
                public Mutiny.Session clear()
                {
                    return null;
                }

                @Override
                public Mutiny.Session enableFetchProfile(String name)
                {
                    return null;
                }

                @Override
                public <T> ResultSetMapping<T> getResultSetMapping(Class<T> resultType, String mappingName)
                {
                    return null;
                }

                @Override
                public <T> EntityGraph<T> getEntityGraph(Class<T> rootType, String graphName)
                {
                    return null;
                }

                @Override
                public <T> EntityGraph<T> createEntityGraph(Class<T> rootType)
                {
                    return null;
                }

                @Override
                public <T> EntityGraph<T> createEntityGraph(Class<T> rootType, String graphName)
                {
                    return null;
                }

                @Override
                public Mutiny.Session disableFetchProfile(String name)
                {
                    return null;
                }

                @Override
                public boolean isFetchProfileEnabled(String name)
                {
                    return false;
                }

                @Override
                public Mutiny.Session setDefaultReadOnly(boolean readOnly)
                {
                    return null;
                }

                @Override
                public boolean isDefaultReadOnly()
                {
                    return false;
                }

                @Override
                public Mutiny.Session setReadOnly(Object entityOrProxy, boolean readOnly)
                {
                    return null;
                }

                @Override
                public boolean isReadOnly(Object entityOrProxy)
                {
                    return false;
                }

                @Override
                public Mutiny.Session setCacheMode(CacheMode cacheMode)
                {
                    return null;
                }

                @Override
                public CacheMode getCacheMode()
                {
                    return null;
                }

                @Override
                public Mutiny.Session setBatchSize(Integer batchSize)
                {
                    return null;
                }

                @Override
                public Integer getBatchSize()
                {
                    return 0;
                }

                @Override
                public Filter enableFilter(String filterName)
                {
                    return null;
                }

                @Override
                public void disableFilter(String filterName)
                {

                }

                @Override
                public Filter getEnabledFilter(String filterName)
                {
                    return null;
                }

                @Override
                public int getFetchBatchSize()
                {
                    return 0;
                }

                @Override
                public Mutiny.Session setFetchBatchSize(int batchSize)
                {
                    return null;
                }

                @Override
                public boolean isSubselectFetchingEnabled()
                {
                    return false;
                }

                @Override
                public Mutiny.Session setSubselectFetchingEnabled(boolean enabled)
                {
                    return null;
                }

                @Override
                public <T> Uni<T> withTransaction(Function<Mutiny.Transaction, Uni<T>> work)
                {
                    return null;
                }

                @Override
                public Mutiny.Transaction currentTransaction()
                {
                    return null;
                }

                @Override
                public boolean isOpen()
                {
                    return false;
                }

                @Override
                public Mutiny.SessionFactory getFactory()
                {
                    return null;
                }

                @Override
                public Uni<Void> close()
                {
                    return null;
                }
            };
        }
        if (unitOfWork instanceof JtaUnitOfWork jtaUnitOfWork)
        {
            // Check if we're in a call scope and handle transaction context transfer
            CallScoper callScoper = IGuiceContext.get(CallScoper.class);
            if (callScoper.isStartedScope())
            {
                CallScopeProperties csp = IGuiceContext.get(CallScopeProperties.class);
                if (csp.getProperties().containsKey("startedOnThisThread"))
                {
                    boolean startedOnThisThread = (boolean) csp.getProperties().get("startedOnThisThread");
                    if (startedOnThisThread && !isUnitOfWorkActive())
                    {
                        unitOfWork.begin();
                    }else {
                        throw new UnsupportedOperationException("No Unit of Work started to manage this request for a session. Please wrap the call in a transaction");
                    }
                }
                return jtaUnitOfWork.getSession().await().atMost(Duration.of(1, ChronoUnit.MINUTES));
            }
            else
            {
                callScoper.enter();
                unitOfWork.begin();
                return jtaUnitOfWork.getSession().await().atMost(Duration.of(1, ChronoUnit.MINUTES));
            }
        }
        else
        {
            throw new UnsupportedOperationException("Can't provide a Mutiny.Session without a JtaUnitOfWork. Please wrap the call in a transaction");
        }
    }

    private boolean isUnitOfWorkActive()
    {
        CallScopeProperties csp = IGuiceContext.get(CallScopeProperties.class);
        return csp.getProperties().get(JtaPersistService.ENTITY_MANAGER_KEY) != null;
    }
}
