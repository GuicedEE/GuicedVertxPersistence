package com.guicedee.persistence.implementations;

import com.guicedee.vertx.spi.VertXPreStartup;
import io.vertx.core.Vertx;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.reactive.vertx.VertxInstance;
import org.hibernate.service.spi.ServiceContributor;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.bytecode.internal.BytecodeProviderInitiator;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.cfg.BytecodeSettings;
import org.hibernate.service.spi.ServiceRegistryImplementor;
import java.util.Map;
import java.util.ServiceLoader;

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
        serviceRegistryBuilder.addInitiator(new StandardServiceInitiator<BytecodeProvider>() {
            @Override public Class<BytecodeProvider> getServiceInitiated() { return BytecodeProvider.class; }
            @Override public BytecodeProvider initiateService(Map<String, Object> settings, ServiceRegistryImplementor registry) {
                Object explicit = settings.get(BytecodeSettings.BYTECODE_PROVIDER_INSTANCE);
                if (explicit instanceof BytecodeProvider provider) return provider;
                if (explicit != null) throw new IllegalArgumentException(BytecodeSettings.BYTECODE_PROVIDER_INSTANCE + " must be a BytecodeProvider");
                if (!Boolean.parseBoolean(String.valueOf(settings.getOrDefault("guicedee.persistence.optimize_enhanced_entities", true))))
                    return BytecodeProviderInitiator.INSTANCE.initiateService(settings, registry);
                if (Boolean.TRUE.equals(settings.get("guicedee.persistence.standard_loader"))) {
                    // The aggregate loader doesn't enumerate named-module services. In
                    // the known default-loader arrangement, inspect the defining loader
                    // instead; Provider.type() does not construct Byte Buddy.
                    var providers = ServiceLoader.load(BytecodeProvider.class, BytecodeProvider.class.getClassLoader()).stream().toList();
                    if (providers.size() == 1 && providers.getFirst().type().getName().equals(
                            "org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl"))
                        return new EnhancedEntityBytecodeProvider(providers.getFirst()::get);
                }
                // Custom loader arrangements retain Hibernate's aggregate discovery
                // and the exact provider it supplies, including its configuration.
                return BytecodeProviderInitiator.INSTANCE.initiateService(settings, registry);
            }
        });
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
