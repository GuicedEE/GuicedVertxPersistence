package com.guicedee.vertxpersistence.implementations;

import com.guicedee.vertx.spi.VertXPreStartup;
import io.vertx.core.Vertx;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.reactive.vertx.VertxInstance;
import org.hibernate.service.spi.ServiceContributor;

/**
 * Provides the Vert.x instance to hibernate reactive
 */
public class VertxServiceContributor implements ServiceContributor
{
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
