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
import com.google.common.collect.Lists;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.guicedee.vertxpersistence.ConnectionBaseInfo;
import com.guicedee.vertxpersistence.PersistService;
import com.guicedee.vertxpersistence.annotations.InvalidConnectionInfoException;
import org.hibernate.reactive.mutiny.Mutiny;

import java.util.List;
import java.util.Map;

/**
 * JPA provider for guice persist.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public final class JtaPersistModule extends PersistModule
{
    private final String jpaUnit;
    private final ConnectionBaseInfo connectionBaseInfo;

    private final com.guicedee.vertxpersistence.annotations.EntityManager annotation;

    private static boolean defaultSet = false;

    public JtaPersistModule(String jpaUnit, ConnectionBaseInfo connectionBaseInfo, com.guicedee.vertxpersistence.annotations.EntityManager annotation)
    {
        this.annotation = annotation;
        Preconditions.checkArgument(
                null != jpaUnit && jpaUnit.length() > 0, "JPA unit name must be a non-empty string.");
        this.jpaUnit = jpaUnit;
        this.connectionBaseInfo = connectionBaseInfo;
    }

    private Map<?, ?> properties;

    /**
     * Returns a Key for the given class with the jpaUnit as the named binding.
     *
     * @param clazz The class to create a Key for
     * @return A Key for the given class with the jpaUnit as the named binding
     */
    private <T> Key<T> getKey(Class<T> clazz)
    {
        return Key.get(clazz, Names.named(jpaUnit));
    }

    /**
     * Returns a List of Keys for the given class.
     * The first Key uses the jpaUnit as the named binding.
     *
     * @param clazz The class to create Keys for
     * @return A List of Keys for the given class
     */
    private <T> List<Key<T>> getKeys(Class<T> clazz)
    {
        List<Key<T>> keys = Lists.newArrayList();
        keys.add(Key.get(clazz, Names.named(jpaUnit)));
        return keys;
    }

    @Override
    protected void configurePersistence()
    {
        JtaPersistService ps = new JtaPersistService(jpaUnit, properties);
        // Create a direct provider for Mutiny.SessionFactory
        MutinySessionFactoryProvider mutinySessionFactoryProvider = new MutinySessionFactoryProvider(ps);

        // Bind with both keys (named and annotation)
        for (Key<Map> key : getKeys(Map.class))
        {
            bind(key).toInstance(properties);
        }

        for (Key<JtaPersistService> key : getKeys(JtaPersistService.class))
        {
            bind(key).toInstance(ps);
        }

        Key<JtaPersistService> jtaPersistServiceKey = getKey(JtaPersistService.class);

        for (Key<PersistService> key : getKeys(PersistService.class))
        {
            bind(key).to(jtaPersistServiceKey);
        }

        // Check if the connection is reactive
        if (connectionBaseInfo.isReactive())
        {
            // Bind Mutiny.SessionFactory with the provider
            for (Key<Mutiny.SessionFactory> key : getKeys(Mutiny.SessionFactory.class))
            {
                bind(key).toProvider(mutinySessionFactoryProvider);
            }

            // Create EntityManagerFactory provider that also sets the factory on JtaUnitOfWork
           /* JtaPersistService.EntityManagerFactoryProvider entityManagerFactoryProvider =
                new JtaPersistService.EntityManagerFactoryProvider(ps) {
                    @Override
                    public EntityManagerFactory get() {
                        EntityManagerFactory factory = super.get();
                        unitOfWork.setEntityManagerFactory(factory);
                        return factory;
                    }
                };*/

           /* for (Key<EntityManagerFactory> key : getKeys(EntityManagerFactory.class)) {
                bind(key).toProvider(entityManagerFactoryProvider);
            }*/
        }

        if (!defaultSet && connectionBaseInfo.isDefaultConnection())
        {
            defaultSet = true;
            // For default bindings, we use the named key
            //   bind(EntityManagerFactory.class).to(getKey(EntityManagerFactory.class));
            //    bind(EntityManager.class).to(getKey(EntityManager.class));
            bind(PersistService.class).to(getKey(PersistService.class));
            //   bind(JtaPersistOptions.class).to(getKey(JtaPersistOptions.class));
            bind(Mutiny.SessionFactory.class).toProvider(mutinySessionFactoryProvider);
            //  bind(Mutiny.Session.class).toProvider(MutinySessionProvider.class);
        }
        else if (defaultSet && connectionBaseInfo.isDefaultConnection())
        {
            throw new InvalidConnectionInfoException("Cannot have two database connection information set as default - " + jpaUnit + " - " + connectionBaseInfo);
        }
    }

    /**
     * Configures the JPA persistence provider with a set of properties.
     *
     * @param properties A set of name value pairs that configure a JPA persistence provider as per
     *                   the specification.
     * @since 4.0 (since 3.0 with a parameter type of {@code java.util.Properties})
     */
    public JtaPersistModule properties(Map<?, ?> properties)
    {
        this.properties = properties;
        return this;
    }

    private final List<Class<?>> dynamicFinders = Lists.newArrayList();

    /**
     * Adds an interface to this module to use as a dynamic finder.
     *
     * @param iface Any interface type whose methods are all dynamic finders.
     */
    public <T> JtaPersistModule addFinder(Class<T> iface)
    {
        dynamicFinders.add(iface);
        return this;
    }

}
