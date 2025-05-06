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

package com.guicedee.vertxpersistence.bind;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;
import com.guicedee.client.IGuiceContext;
import com.guicedee.guicedservlets.servlets.services.scopes.CallScope;
import com.guicedee.client.CallScopeProperties;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import lombok.Getter;
import org.hibernate.reactive.loader.ast.internal.ReactiveAbstractCollectionBatchLoader;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.Map;

/**
 * @author Dhanji R. Prasanna (dhanji@gmail.com)
 */
public class JtaPersistService implements Provider<EntityManager>, UnitOfWork, PersistService
{
    private final String persistenceUnitName;
    private final Map<?, ?> persistenceProperties;
    private final JtaPersistOptions options;
    @Inject
    private JtaUnitOfWork unitOfWork;
    private boolean isReactive;

    // Entity manager key for CallScopeProperties
    public static final String ENTITY_MANAGER_KEY = "entityManager";

    public JtaPersistService(
            JtaPersistOptions options,
            String persistenceUnitName,
            Map<?, ?> persistenceProperties)
    {
        this.options = options;
        this.persistenceUnitName = persistenceUnitName;
        this.persistenceProperties = persistenceProperties;
    }

    @Override
    public EntityManager get()
    {
        if (options.getAutoBeginWorkOnEntityManagerCreation() && !isWorking())
        {
            begin();
        }

        CallScopeProperties csp = IGuiceContext.get(CallScopeProperties.class);
        EntityManager em = (EntityManager) csp.getProperties().get(ENTITY_MANAGER_KEY);
        Preconditions.checkState(
                null != em,
                "Requested EntityManager outside work unit. As of Guice 6.0, Guice Persist doesn't"
                        + " automatically begin the unit of work when provisioning an EntityManager. To"
                        + " preserve the legacy behavior, construct the `JpaPersistModule` with a"
                        + " `JpaPersistOptions.builder().setAutoBeginWorkOnEntityManagerCreation(true).build()`."
                        + " Alternately, try calling UnitOfWork.begin() first, or use a PersistFilter if you"
                        + " are inside a servlet environment.");

        return em;
    }

    public boolean isWorking()
    {
        CallScopeProperties csp = IGuiceContext.get(CallScopeProperties.class);
        return csp.getProperties().get(ENTITY_MANAGER_KEY) != null;
    }

    /**
     * Sets the JtaUnitOfWork for this service and marks it as reactive
     * 
     * @param unitOfWork The JtaUnitOfWork to use
     * @param isReactive Whether the connection is reactive
     */
    public void setUnitOfWork(JtaUnitOfWork unitOfWork, boolean isReactive) {
        this.unitOfWork = unitOfWork;
        this.isReactive = isReactive;
    }

    @Override
    public void begin()
    {
        CallScopeProperties csp = IGuiceContext.get(CallScopeProperties.class);
        Preconditions.checkState(
                null == csp.getProperties().get(ENTITY_MANAGER_KEY),
                "Work already begun on this thread. Looks like you have called UnitOfWork.begin() twice"
                        + " without a balancing call to end() in between.");

        if (isReactive && unitOfWork != null) {
            // Delegate to JtaUnitOfWork for reactive connections
            unitOfWork.begin();
        } else {
            // Use standard EntityManager for non-reactive connections
            var em = emFactory.createEntityManager();
            csp.getProperties().put(ENTITY_MANAGER_KEY, em);
        }
    }

    @Override
    public void end()
    {
        if (isReactive && unitOfWork != null) {
            // Delegate to JtaUnitOfWork for reactive connections
            unitOfWork.end();
            return;
        }

        CallScopeProperties csp = IGuiceContext.get(CallScopeProperties.class);
        EntityManager em = (EntityManager) csp.getProperties().get(ENTITY_MANAGER_KEY);

        // Let's not penalize users for calling end() multiple times.
        if (null == em)
        {
            return;
        }

        try
        {
            em.close();
        }
        finally
        {
            csp.getProperties().remove(ENTITY_MANAGER_KEY);
        }
    }

    @Getter
    private volatile EntityManagerFactory emFactory;

    @Override
    public synchronized void start()
    {
        if (null != emFactory)
        {
            return;
        }

        if (null != persistenceProperties)
        {
            this.emFactory =
                    Persistence.createEntityManagerFactory(persistenceUnitName, persistenceProperties);
            if(this.isReactive && this.unitOfWork != null) {
                var emfr = this.emFactory
                        .unwrap(Mutiny.SessionFactory.class);
                this.unitOfWork.setEntityManagerFactory((org.hibernate.reactive.mutiny.impl.MutinySessionFactoryImpl) emfr);
            }
        }
        else
        {
            this.emFactory = Persistence.createEntityManagerFactory(persistenceUnitName);
            if(!this.isReactive && this.unitOfWork != null) {
                this.unitOfWork.setEntityManagerFactory(this.emFactory);
            }
        }
    }

    @Override
    public synchronized void stop()
    {
        if (null != emFactory && emFactory.isOpen())
        {
            emFactory.close();
        }
    }

    public static class EntityManagerFactoryProvider implements Provider<EntityManagerFactory>
    {
        private final JtaPersistService emProvider;

        public EntityManagerFactoryProvider(JtaPersistService emProvider)
        {
            this.emProvider = emProvider;
        }

        @Override
        public EntityManagerFactory get()
        {
            assert null != emProvider.emFactory;
            return emProvider.emFactory;
        }
    }
}
