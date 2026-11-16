package com.guicedee.persistence.implementations;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.property.access.spi.PropertyAccess;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Enhanced entities already have attribute interceptors. Use Hibernate's standard
 * property access for them instead of generating another pair of accessor classes.
 * Runtime proxies, enhancement, embeddables and unenhanced classes retain the upstream
 * provider. Its creation is deferred until one of those operations actually needs it.
 */
@SuppressWarnings({"removal", "rawtypes"})
public final class EnhancedEntityBytecodeProvider implements BytecodeProvider {
    private final Supplier<BytecodeProvider> factory;
    private volatile BytecodeProvider delegate;

    public EnhancedEntityBytecodeProvider(Supplier<BytecodeProvider> factory) {
        this.factory = factory;
    }

    private BytecodeProvider delegate() {
        BytecodeProvider current = delegate;
        if (current == null) {
            synchronized (this) {
                current = delegate;
                if (current == null) delegate = current = factory.get();
            }
        }
        return current;
    }

    @Override public ReflectionOptimizer getReflectionOptimizer(Class type, String[] getters, String[] setters, Class[] types) {
        return PersistentAttributeInterceptable.class.isAssignableFrom(type) ? null
                : delegate().getReflectionOptimizer(type, getters, setters, types);
    }

    @Override public ReflectionOptimizer getReflectionOptimizer(Class<?> type, Map<String, PropertyAccess> access) {
        return PersistentAttributeInterceptable.class.isAssignableFrom(type) ? null
                : delegate().getReflectionOptimizer(type, access);
    }

    @Override public ProxyFactoryFactory getProxyFactoryFactory() { return delegate().getProxyFactoryFactory(); }
    @Override public Enhancer getEnhancer(EnhancementContext context) { return delegate().getEnhancer(context); }
    @Override public void resetCaches() {
        BytecodeProvider current = delegate;
        if (current != null) current.resetCaches();
    }
}
