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

import com.google.inject.Provider;
import com.guicedee.vertxpersistence.PersistService;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.util.Map;

/**
 * @author Dhanji R. Prasanna (dhanji@gmail.com)
 */
@Log4j2
public class JtaPersistService implements PersistService
{
    private final String persistenceUnitName;
    @Getter
    private final Map<?, ?> persistenceProperties;

    public JtaPersistService(
            String persistenceUnitName,
            Map<?, ?> persistenceProperties)
    {
        log.debug("📋 Creating JtaPersistService for persistence unit: '{}'", persistenceUnitName);
        this.persistenceUnitName = persistenceUnitName;
        this.persistenceProperties = persistenceProperties;
    }

    private volatile EntityManagerFactory emFactory;

    public EntityManagerFactory getEmFactory()
    {
        log.debug("📋 Getting EntityManagerFactory for persistence unit: '{}'", persistenceUnitName);
        if (emFactory == null)
        {
            log.debug("🔄 EntityManagerFactory not initialized, starting JtaPersistService for persistence unit: '{}'", persistenceUnitName);
            start();
        }
        log.debug("📤 Returning EntityManagerFactory for persistence unit: '{}'", persistenceUnitName);
        return emFactory;
    }

    @Override
    public synchronized void start()
    {
        log.info("🚀 Starting JtaPersistService for persistence unit: '{}'", persistenceUnitName);
        if (null != emFactory)
        {
            log.debug("📋 EntityManagerFactory already exists for persistence unit: '{}', skipping initialization", persistenceUnitName);
            return;
        }

        if (null != persistenceProperties)
        {
            try {
                log.debug("📋 Creating EntityManagerFactory for persistence unit: '{}'", persistenceUnitName);
                this.emFactory =
                        Persistence.createEntityManagerFactory(persistenceUnitName, persistenceProperties);
                log.info("✅ Successfully created EntityManagerFactory for persistence unit: '{}'", persistenceUnitName);
/*                if(this.unitOfWork != null) {
                    var emfr = this.emFactory
                            .unwrap(Mutiny.SessionFactory.class);
                    this.unitOfWork.setEntityManagerFactory((org.hibernate.reactive.mutiny.impl.MutinySessionFactoryImpl) emfr);
                }*/
            } catch (Exception e) {
                log.error("❌ Failed to create EntityManagerFactory for persistence unit: '{}': {}", 
                    persistenceUnitName, e.getMessage(), e);
                throw e;
            }
        } else {
            log.warn("⚠️ No persistence properties provided for persistence unit: '{}'", persistenceUnitName);
        }
    }

    @Override
    public synchronized void stop()
    {
        log.info("🛑 Stopping JtaPersistService for persistence unit: '{}'", persistenceUnitName);
        if (null != emFactory && emFactory.isOpen())
        {
            try {
                log.debug("📋 Closing EntityManagerFactory for persistence unit: '{}'", persistenceUnitName);
                emFactory.close();
                log.info("✅ Successfully closed EntityManagerFactory for persistence unit: '{}'", persistenceUnitName);
            } catch (Exception e) {
                log.error("❌ Failed to close EntityManagerFactory for persistence unit: '{}': {}", 
                    persistenceUnitName, e.getMessage(), e);
            }
        } else {
            log.debug("📋 No open EntityManagerFactory to close for persistence unit: '{}'", persistenceUnitName);
        }
    }

    public static class EntityManagerFactoryProvider implements Provider<EntityManagerFactory>
    {
        private static final org.apache.logging.log4j.Logger log = org.apache.logging.log4j.LogManager.getLogger(EntityManagerFactoryProvider.class);
        private final JtaPersistService emProvider;

        public EntityManagerFactoryProvider(JtaPersistService emProvider)
        {
            this.emProvider = emProvider;
            log.debug("📋 Created EntityManagerFactoryProvider for persistence unit: '{}'", emProvider.persistenceUnitName);
        }

        @Override
        public EntityManagerFactory get()
        {
            log.debug("📋 Provider requested EntityManagerFactory for persistence unit: '{}'", emProvider.persistenceUnitName);
            assert null != emProvider.emFactory : "EntityManagerFactory is null for persistence unit: " + emProvider.persistenceUnitName;
            log.debug("📤 Providing EntityManagerFactory for persistence unit: '{}'", emProvider.persistenceUnitName);
            return emProvider.emFactory;
        }
    }
}
