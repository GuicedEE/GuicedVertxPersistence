package com.guicedee.vertxpersistence.implementations;

import com.guicedee.vertx.spi.VertXPreStartup;
import io.vertx.core.Vertx;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.reactive.vertx.VertxInstance;
import org.hibernate.service.spi.ServiceContributor;

/**
 * Bridges the shared Vert.x instance into Hibernate Reactive's service registry.
 */
public class VertxServiceContributor implements ServiceContributor
{
    /**
     * Registers a {@link VertxInstance} backed by the application Vert.x singleton.
     *
     * @param serviceRegistryBuilder the Hibernate service registry builder
     */
    @Override
    public void contribute(StandardServiceRegistryBuilder serviceRegistryBuilder)
    {
        serviceRegistryBuilder.addService(VertxInstance.class, new VertxInstance()
        {
            @Override
            public Vertx getVertx()
            {
                return VertXPreStartup.getVertx();
            }
        });
    }
}
