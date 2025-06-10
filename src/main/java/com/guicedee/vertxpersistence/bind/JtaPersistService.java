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
import com.google.inject.Singleton;
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
public class JtaPersistService implements PersistService
{
    private final String persistenceUnitName;
    @Getter
    private final Map<?, ?> persistenceProperties;

    public JtaPersistService(
            String persistenceUnitName,
            Map<?, ?> persistenceProperties)
    {
        this.persistenceUnitName = persistenceUnitName;
        this.persistenceProperties = persistenceProperties;
    }

    private volatile EntityManagerFactory emFactory;

    public EntityManagerFactory getEmFactory()
    {
        if (emFactory == null)
        {
            start();
        }
        return emFactory;
    }

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
/*            if(this.unitOfWork != null) {
                var emfr = this.emFactory
                        .unwrap(Mutiny.SessionFactory.class);
                this.unitOfWork.setEntityManagerFactory((org.hibernate.reactive.mutiny.impl.MutinySessionFactoryImpl) emfr);
            }*/
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
